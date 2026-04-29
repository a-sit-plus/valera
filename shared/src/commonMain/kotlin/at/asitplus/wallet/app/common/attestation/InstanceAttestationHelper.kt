package at.asitplus.wallet.app.common.attestation

import at.asitplus.attestation.supreme.AttestationChallenge
import at.asitplus.attestation.supreme.AttestationClient
import at.asitplus.attestation.supreme.createCsr
import at.asitplus.openid.ClientNonceResponse
import at.asitplus.signum.indispensable.asn1.ObjectIdentifier
import at.asitplus.signum.indispensable.asn1.encoding.encodeToAsn1Primitive
import at.asitplus.signum.indispensable.josef.JsonWebToken
import at.asitplus.signum.indispensable.josef.JwsSigned
import at.asitplus.signum.indispensable.pki.Pkcs10CertificationRequestAttribute
import at.asitplus.signum.supreme.os.PlatformSigningProvider
import at.asitplus.wallet.app.common.HttpService
import at.asitplus.wallet.lib.jws.JwsContentTypeConstants.CLIENT_ATTESTATION_POP_JWT
import at.asitplus.wallet.lib.jws.JwsHeaderNone
import at.asitplus.wallet.lib.jws.SignJwt
import at.asitplus.wallet.lib.jws.SignJwtFun
import io.ktor.client.call.body
import io.ktor.client.request.get
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
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

class InstanceAttestationHelper(val host: Flow<String>, httpService: HttpService) {
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
    val nonceEndpoint = host.map {
        URLBuilder(host.first()).apply {
            appendEncodedPathSegments(PATH_NONCE)
        }
    }
    val httpClient = httpService.buildHttpClient()
    val client = AttestationClient(httpClient)

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
    private suspend fun getNonce() = runCatching {
        httpClient.get(Url(nonceEndpoint.first())) {
        }.body<ClientNonceResponse>().clientNonce
    }


    suspend fun requestInstanceAttestation(versionName: String) =
        getAttestationChallenge().getOrThrow().let { challenge ->
            PlatformSigningProvider.deleteSigningKey(KS_ALIAS_WIA)
            val instanceAttestationSigner = createAttestationSigner(challenge, KS_ALIAS_WIA)
            val csr = instanceAttestationSigner.createCsr(
                challenge = challenge, additionalAttributes = listOf(
                    Pkcs10CertificationRequestAttribute(
                        oid = ObjectIdentifier(oid = WALLET_SOLUTION_OID), listOf(
                            versionName.encodeToAsn1Primitive()
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

    suspend fun buildProofOfPossession(nonce: String? = null): JwsSigned<JsonWebToken> =
        PlatformSigningProvider.getSignerForKey(KS_ALIAS_WIA).getOrThrow().let {
            BuildInstanceAttestationProofJwt(
                SignJwt(HolderKeyMaterial(it), headerModifier = JwsHeaderNone()),
                lifetime = 1.minutes,
                audience = null,
                nonce = nonce ?: getNonce().getOrNull()
            )
        }
}

object BuildInstanceAttestationProofJwt {
    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke(
        signJwt: SignJwtFun<JsonWebToken>,
        audience: String? = null,
        nonce: String? = null,
        lifetime: Duration = 60.minutes,
        clockSkew: Duration = 5.minutes,
    ) = signJwt(
        CLIENT_ATTESTATION_POP_JWT,
        JsonWebToken(
            audience = audience,
            nonce = nonce,
            issuedAt = Clock.System.now() - clockSkew,
            expiration = Clock.System.now() + lifetime
        ),
        JsonWebToken.Companion.serializer(),
    ).getOrThrow()
}