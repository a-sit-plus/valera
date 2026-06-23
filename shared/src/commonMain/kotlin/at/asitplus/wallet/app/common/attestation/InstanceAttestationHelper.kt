package at.asitplus.wallet.app.common.attestation

import at.asitplus.attestation.supreme.AttestationClient
import at.asitplus.attestation.supreme.createAttestationProof
import at.asitplus.catchingUnwrapped
import at.asitplus.signum.indispensable.asn1.ObjectIdentifier
import at.asitplus.signum.indispensable.asn1.encoding.encodeToAsn1Primitive
import at.asitplus.signum.indispensable.josef.JsonWebToken
import at.asitplus.signum.indispensable.josef.JwsCompactTyped
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.signum.indispensable.pki.Pkcs10CertificationRequestAttribute
import at.asitplus.signum.indispensable.pki.X509Certificate
import at.asitplus.signum.supreme.os.PlatformSigningProvider
import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.data.SettingsRepository
import at.asitplus.wallet.lib.agent.KeyMaterial
import at.asitplus.wallet.lib.agent.SignerBasedKeyMaterial
import at.asitplus.wallet.lib.jws.JwsHeaderNone
import at.asitplus.wallet.lib.jws.SignJwt
import at.asitplus.wallet.lib.oidvci.BuildClientAttestationPoPJwt
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.appendEncodedPathSegments
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class InstanceAttestationHelper(
    private val config: SettingsRepository,
    private val httpClient: HttpClient,
    private val buildContext: BuildContext,
) {
    private fun challengeEndpoint() = config.walletProviderHost.map {
        URLBuilder(it).apply {
            appendEncodedPathSegments(PATH_CHALLENGE)
        }
    }
    private fun instanceEndpoint() = config.walletProviderHost.map {
        URLBuilder(it).apply {
            appendEncodedPathSegments(PATH_INSTANCE)
        }
    }

    suspend fun reset() = withInstanceAttestationContext {
        clearCachedInstanceAttestation()
    }

    private val client = AttestationClient(httpClient)
    private val instanceAttestationDispatcher = Dispatchers.IO.limitedParallelism(1)

    private var instanceAttestationCache: InstanceAttestationMaterial? = null

    suspend fun instanceAttestation(
        preferredClientStatusPeriod: Duration
    ) = withInstanceAttestationContext {
        validCachedInstanceAttestationOrNull()?.attestation
            ?: getVerifiedInstanceAttestation(preferredClientStatusPeriod).attestation
    }

    suspend fun keyMaterial() = withInstanceAttestationContext {
        validCachedInstanceAttestationOrNull()?.keyMaterial
            ?: getVerifiedInstanceAttestation(PREFERRED_DEFAULT_TTL).keyMaterial
    }

    fun currentInstanceAttestation(): JwsCompactTyped<JsonWebToken>? = instanceAttestationCache?.attestation

    /**
     * Restores a previously persisted instance [attestation] (e.g. across a provisioning browser flow)
     * into the cache. The restored attestation is paired with the current WIA key material and only
     * cached when its `cnf.jwk` still matches that key, mirroring [validCachedInstanceAttestationOrNull]
     * so that a restored attestation can never diverge from the key that signs its proof of possession.
     */
    suspend fun restoreInstanceAttestation(
        attestation: JwsCompactTyped<JsonWebToken>
    ) = withInstanceAttestationContext {
        val keyMaterial = catchingUnwrapped { signerBasedKeyMaterial() }.getOrElse {
            Napier.w("Cannot restore instance attestation, WIA key unavailable: $it")
            return@withInstanceAttestationContext
        }
        val restored = InstanceAttestationMaterial(attestation, keyMaterial)
        if (restored.hasMatchingConfirmationKey()) {
            instanceAttestationCache = restored
            Napier.d("Restored cached instance attestation")
        } else {
            Napier.w("Discarding restored instance attestation. ${restored.confirmationKeyMismatchMessage()}")
            clearCachedInstanceAttestation()
        }
    }

    suspend fun instanceAttestationWithProofOfPossession(
        preferredClientStatusPeriod: Duration,
        audience: String,
        nonce: String?,
    ): Pair<JwsCompactTyped<JsonWebToken>, JwsCompactTyped<JsonWebToken>> = withInstanceAttestationContext {
        val instanceAttestation = validCachedInstanceAttestationOrNull()
            ?: getVerifiedInstanceAttestation(preferredClientStatusPeriod)
        instanceAttestation.attestation to buildProofOfPossession(
            keyMaterial = instanceAttestation.keyMaterial,
            audience = audience,
            nonce = nonce,
        )
    }

    private suspend fun <T> withInstanceAttestationContext(block: suspend () -> T): T =
        withContext(instanceAttestationDispatcher) { block() }

    private fun clearCachedInstanceAttestation() {
        instanceAttestationCache = null
    }

    private fun validCachedInstanceAttestationOrNull(): InstanceAttestationMaterial? =
        instanceAttestationCache?.takeIf { it.hasMatchingConfirmationKey() } ?: run {
            instanceAttestationCache?.let {
                Napier.w("Discarding cached instance attestation. ${it.confirmationKeyMismatchMessage()}")
                clearCachedInstanceAttestation()
            }
            null
        }

    private suspend fun getVerifiedInstanceAttestation(
        preferredClientStatusPeriod: Duration
    ): InstanceAttestationMaterial {
        var mismatch: IllegalStateException? = null
        repeat(MAX_INSTANCE_ATTESTATION_ATTEMPTS) {
            val instanceAttestation = getInstanceAttestation(preferredClientStatusPeriod)
            if (instanceAttestation.hasMatchingConfirmationKey()) {
                instanceAttestationCache = instanceAttestation
                return instanceAttestation
            }

            mismatch = IllegalStateException(instanceAttestation.confirmationKeyMismatchMessage())
            Napier.w("Discarding newly loaded instance attestation. ${mismatch.message}")
            clearCachedInstanceAttestation()
        }
        throw mismatch ?: IllegalStateException("Unable to load instance attestation")
    }

    private fun InstanceAttestationMaterial.hasMatchingConfirmationKey() =
        attestation.payload.confirmationClaim?.jsonWebKey?.jwkThumbprint == keyMaterial.jsonWebKey.jwkThumbprint

    private fun InstanceAttestationMaterial.confirmationKeyMismatchMessage() =
        "Instance attestation cnf.jwk does not match WIA signing key. " +
                "Expected cnf thumbprint: ${attestation.payload.confirmationClaim?.jsonWebKey?.jwkThumbprint}, " +
                "got keyMaterial thumbprint: ${keyMaterial.jsonWebKey.jwkThumbprint}"

    private suspend fun getAttestationChallenge() = client.getChallenge(Url(challengeEndpoint().first()))

    private suspend fun getAttestationProof(preferredClientStatusPeriod: Duration) =
        getAttestationChallenge().getOrThrow().let { challenge ->
            PlatformSigningProvider.deleteSigningKey(alias = KS_ALIAS_WIA)
            challenge.createAttestationProof(
                alias = KS_ALIAS_WIA, additionalCsrAttributes = listOf(
                    Pkcs10CertificationRequestAttribute(
                        oid = ObjectIdentifier(oid = WALLET_SOLUTION_OID), listOf(
                            joseCompliantSerializer.encodeToString(
                                InstanceAttestationRequest(
                                    buildContext.versionName, preferredClientStatusPeriod
                                )
                            ).encodeToAsn1Primitive(),
                        )
                    )
                )
            ).getOrThrow()
        }

    private suspend fun getInstanceAttestation(
        preferredClientStatusPeriod: Duration
    ) = getAttestationProof(preferredClientStatusPeriod).let { proof ->
        httpClient.post(Url(instanceEndpoint().first())) {
            contentType(ContentType.Application.OctetStream)
            setBody(proof.encodeToDer())
        }.let { response ->
            when (response.status) {
                HttpStatusCode.OK -> {
                    val keyMaterial = signerBasedKeyMaterial()
                    val attestation = catchingUnwrapped {
                        JwsCompactTyped<JsonWebToken>(response.bodyAsText())
                    }.getOrThrow()
                    InstanceAttestationMaterial(attestation, keyMaterial)
                }
                else -> {
                    throw Throwable(message = "Server responded with an error", cause = Throwable(response.bodyAsText()))
                }
            }
        }
    }

    private suspend fun signerBasedKeyMaterial() =
        PlatformSigningProvider.getSignerForKey(KS_ALIAS_WIA).getOrThrow().let {
            object : SignerBasedKeyMaterial(it) {
                override suspend fun getCertificate(): X509Certificate? = null
            }
        }

    suspend fun buildProofOfPossession(
        audience: String,
        nonce: String?,
    ): JwsCompactTyped<JsonWebToken> = withInstanceAttestationContext {
        val instanceAttestation = validCachedInstanceAttestationOrNull()
            ?: getVerifiedInstanceAttestation(PREFERRED_DEFAULT_TTL)
        buildProofOfPossession(
            keyMaterial = instanceAttestation.keyMaterial,
            audience = audience,
            nonce = nonce,
        )
    }

    private suspend fun buildProofOfPossession(
        keyMaterial: KeyMaterial,
        audience: String,
        nonce: String?,
    ): JwsCompactTyped<JsonWebToken> = BuildClientAttestationPoPJwt.invoke(
        clientId = buildContext.versionName,
        signJwt = SignJwt(keyMaterial, headerModifier = JwsHeaderNone()),
        lifetime = 1.minutes,
        audience = audience,
        nonce = nonce
    )
}

private const val MAX_INSTANCE_ATTESTATION_ATTEMPTS = 2

private data class InstanceAttestationMaterial(
    val attestation: JwsCompactTyped<JsonWebToken>,
    val keyMaterial: KeyMaterial,
)

@Serializable
private data class InstanceAttestationRequest(
    val versionName: String, val preferredClientStatusPeriod: Duration? = null
)
