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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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

    suspend fun reset() {
        instanceAttestationCache = null
        keyMaterialCache = null
    }

    private val client = AttestationClient(httpClient)

    private var instanceAttestationCache: JwsCompactTyped<JsonWebToken>? = null
    private var keyMaterialCache: KeyMaterial? = null

    suspend fun instanceAttestation(
        preferredClientStatusPeriod: Duration
    ) = instanceAttestationCache ?: getInstanceAttestation(preferredClientStatusPeriod).let { (attestation, _) ->
        attestation
    }

    suspend fun keyMaterial() = keyMaterialCache ?: run {
        getInstanceAttestation(PREFERRED_DEFAULT_TTL).let { (_, keyMaterial) ->
            keyMaterial
        }
    }

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
            when(response.status) {
                HttpStatusCode.OK -> {
                    val keyMaterial = signerBasedKeyMaterial().also {
                        keyMaterialCache = it
                    }

                    val attestation = catchingUnwrapped {
                        JwsCompactTyped<JsonWebToken>(response.bodyAsText())
                    }.getOrThrow().also {
                        instanceAttestationCache = it
                    }
                    Pair(attestation, keyMaterial)
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
    ): JwsCompactTyped<JsonWebToken> = BuildClientAttestationPoPJwt.invoke(
        clientId = buildContext.versionName,
        signJwt = SignJwt(keyMaterial(), headerModifier = JwsHeaderNone()),
        lifetime = 1.minutes,
        audience = audience,
        nonce = nonce
    )
}

@Serializable
private data class InstanceAttestationRequest(
    val versionName: String, val preferredClientStatusPeriod: Duration? = null
)