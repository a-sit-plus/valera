package at.asitplus.wallet.app.common.attestation

import at.asitplus.attestation.supreme.AttestationChallenge
import at.asitplus.attestation.supreme.AttestationClient
import at.asitplus.attestation.supreme.createCsr
import at.asitplus.signum.indispensable.asn1.ObjectIdentifier
import at.asitplus.signum.indispensable.asn1.encoding.encodeToAsn1Primitive
import at.asitplus.signum.indispensable.josef.JsonWebToken
import at.asitplus.signum.indispensable.josef.JwsSigned
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.signum.indispensable.pki.Pkcs10CertificationRequestAttribute
import at.asitplus.signum.indispensable.pki.X509Certificate
import at.asitplus.signum.supreme.os.PlatformSigningProvider
import at.asitplus.wallet.app.common.BuildContext
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
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.appendEncodedPathSegments
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class InstanceAttestationHelper(
    val host: Flow<String>,
    val httpClient: HttpClient,
    val buildContext: BuildContext,
) {
    val challengeEndpoint = host.map {
        URLBuilder(host.first()).apply {
            appendEncodedPathSegments(PATH_CHALLENGE)
        }
    }
    val instanceEndpoint = host.map {
        URLBuilder(host.first()).apply {
            appendEncodedPathSegments(PATH_INSTANCE)
        }
    }

    val client = AttestationClient(httpClient)

    val challenge by lazy { runBlocking { getAttestationChallenge().getOrThrow() }
    }

    val instanceAttestationSigner by lazy {
        runBlocking {
            PlatformSigningProvider.deleteSigningKey(KS_ALIAS_WIA)
            val alias = KS_ALIAS_WIA
            createAttestationSigner(challenge, alias)
        }
    }


    fun instanceAttestationKeyMaterial() = object : SignerBasedKeyMaterial(instanceAttestationSigner) {
        override suspend fun getCertificate(): X509Certificate? = null
    }

    suspend fun createAttestationSigner(challenge: AttestationChallenge, alias: String) =
        PlatformSigningProvider.createSigningKey(alias) {
            ec {}
            hardware {
                attestation {
                    this.challenge = challenge.nonce
                }
            }
        }.getOrThrow()

    private suspend fun getAttestationChallenge() = client.getChallenge(Url(challengeEndpoint.first()))

    suspend fun requestInstanceAttestation(
        preferredClientStatusPeriod: Duration
    ) =
        challenge.let { challenge ->
            val csr = instanceAttestationSigner.createCsr(
                challenge = challenge, additionalAttributes = listOf(
                    Pkcs10CertificationRequestAttribute(
                        oid = ObjectIdentifier(oid = WALLET_SOLUTION_OID), listOf(
                            joseCompliantSerializer.encodeToString(
                                InstanceAttestationRequest(
                                    buildContext.versionName,
                                    preferredClientStatusPeriod
                                )
                            ).encodeToAsn1Primitive(),
                        )
                    )
                )
            ).getOrThrow()
            val response = httpClient.post(Url(instanceEndpoint.first())) {
                contentType(ContentType.Application.OctetStream)
                setBody(csr.encodeToDer())
            }
            JwsSigned.deserialize<JsonWebToken>(
                it = response.bodyAsText(),
                deserializationStrategy = JsonWebToken.serializer(),
            ).getOrThrow()
        }

    suspend fun buildProofOfPossession(
        audience: String,
        nonce: String?,
    ): JwsSigned<JsonWebToken> =
        BuildClientAttestationPoPJwt.invoke(
            clientId = buildContext.versionName,
            signJwt = SignJwt(instanceAttestationKeyMaterial(), headerModifier = JwsHeaderNone()),
            lifetime = 1.minutes,
            audience = audience,
            nonce = nonce
        )
}

@Serializable
data class InstanceAttestationRequest(
    val versionName: String,
    val preferredClientStatusPeriod: Duration? = null
)