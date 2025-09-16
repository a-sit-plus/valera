package at.asitplus.wallet.app.common

import at.asitplus.KmmResult
import at.asitplus.attestation.supreme.AttestationChallenge
import at.asitplus.attestation.supreme.AttestationClient
import at.asitplus.attestation.supreme.AttestationResponse
import at.asitplus.attestation.supreme.attestationEndpointUrl
import at.asitplus.attestation.supreme.createCsr
import at.asitplus.openid.odcJsonSerializer
import at.asitplus.signum.indispensable.io.X509CertificateBase64UrlSerializer
import at.asitplus.signum.indispensable.josef.JwsSigned
import at.asitplus.signum.indispensable.pki.Pkcs10CertificationRequest
import at.asitplus.signum.indispensable.pki.X509Certificate
import at.asitplus.signum.supreme.os.PlatformSigningProvider
import at.asitplus.signum.supreme.sign.Signer
import at.asitplus.wallet.lib.agent.EphemeralKeyWithoutCert
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.jws.JwsHeaderNone
import at.asitplus.wallet.lib.jws.SignJwt
import at.asitplus.wallet.lib.jws.SignJwtFun
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Url
import io.ktor.http.contentType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Instant

class AttestationService() {
    val httpClient = HttpClient()
    val ENDPOINT_CHALLENGE = "http://10.0.2.2:8080/api/v1/challenge"
    val KS_ALIAS_CSR = "ALIAS_CSR"
    val KS_ALIAS_WAA = "ALIAS_WAA"

    val client = AttestationClient(httpClient)

    private val signClientAttestationPop: SignJwtFun<WalletAttestationToken>? = SignJwt(
        EphemeralKeyWithoutCert(),
        JwsHeaderNone()
    )
    lateinit var signer: Signer.Attestable<*>

    suspend fun start() {
        Napier.e("AttestationService: start()")
        runCatching {
            waaFlow()
        }.onFailure {
            Napier.e("AttestationService $it")
        }
    }

    suspend fun csrFlow() {
        clearCsrSigningKey()
        val challenge = getChallenge().getOrThrow()
        val csr = createCsr(challenge, KS_ALIAS_CSR).getOrThrow()
        attest(csr, challenge).onSuccess {
            Napier.e(it.toString())
        }.onFailure {
            Napier.e(it.enrichMessage())
        }
    }

    suspend fun waaFlow() {
        clearWaaKey()
        val challenge = getChallenge().getOrThrow()
        Napier.e("Got challenge $challenge")
        val csr = createCsr(challenge, KS_ALIAS_WAA).getOrThrow()
        Napier.e("AttestationService: post")
        val response = httpClient.post("http://10.0.2.2:8080/api/v1/waa") {
            contentType(ContentType.Application.OctetStream)
            setBody(csr.encodeToDer())
        }
        Napier.e("AttestationService: deserialize()")
        JwsSigned.deserialize<WalletAttestationToken>(
            it = response.bodyAsText(),
            deserializationStrategy = WalletAttestationToken.serializer(),
            json = odcJsonSerializer
        ).onSuccess {
            Napier.e("WAA: ${vckJsonSerializer.encodeToString(it.payload)}")
            wuaFlow(it)
        }.onFailure {
            Napier.e("$it")
        }
    }

    suspend fun wuaFlow(token: JwsSigned<WalletAttestationToken>) {
        val response = httpClient.post("http://10.0.2.2:8080/api/v1/wua") {
            contentType(ContentType.Application.Json)
            setBody(token.serialize())
        }
        JwsSigned.deserialize<WalletAttestationToken>(
            it = response.bodyAsText(),
            deserializationStrategy = WalletAttestationToken.serializer(),
            json = odcJsonSerializer
        ).onSuccess {
            Napier.e("WUA: ${vckJsonSerializer.encodeToString(it.payload)}")
        }.onFailure {
            Napier.e("$it")
        }
    }

    suspend fun clearWaaKey() = PlatformSigningProvider.deleteSigningKey(KS_ALIAS_WAA)

    suspend fun createWaaKey() {

    }

    suspend fun clearCsrSigningKey() = PlatformSigningProvider.deleteSigningKey(KS_ALIAS_CSR)

    suspend fun getChallenge() = client.getChallenge(Url(ENDPOINT_CHALLENGE))

    suspend fun createCsr(challenge: AttestationChallenge, alias: String): KmmResult<Pkcs10CertificationRequest> {
        signer = PlatformSigningProvider.createSigningKey(alias) {
            ec {}
            hardware {
                attestation {
                    this.challenge = challenge.nonce
                }
            }
        }.getOrThrow()

        return signer.createCsr(challenge)
    }

    suspend fun attest(
        csr: Pkcs10CertificationRequest,
        challenge: AttestationChallenge
    ): Result<List<@Serializable(with = X509CertificateBase64UrlSerializer::class) X509Certificate>> {
        val response = client.attest(csr, challenge.attestationEndpointUrl)
        return when (response) {
            is AttestationResponse.Success -> {
                Result.success(response.certificateChain)
            }

            is AttestationResponse.Failure -> {
                Result.failure(Throwable(message = "${response.explanation}"))
            }
        }
    }
}

@Serializable
class WalletAttestationToken(
    @SerialName("iss")
    val issuer: String? = null,

    @SerialName("typ")
    val type: String,

    @SerialName("aud")
    val audience: String? = null,

    @SerialName("exp")
    @Serializable(with = InstantLongSerializer::class)
    val expiration: Instant? = null,

    @SerialName("jti")
    val jwtId: String? = null,

    @SerialName("eudi_wallet_info")
    val eudiWalletInfo: GeneralInfo,

    @SerialName("wscd_info")
    val wscdInfo: WscdInfo? = null
)

@Serializable
data class GeneralInfo(
    @SerialName("wallet_provider_name") val walletProviderName: String,
    @SerialName("wallet_solution_id") val walletSolutionId: String,
    @SerialName("wallet_solution_version") val walletSolutionVersion: String,
    @SerialName("wallet_solution_certification_information") val walletSolutionCertificationInformation: String
)

@Serializable
data class WscdInfo(
    @SerialName("wscd_type") val wscdType: String,
    @SerialName("wscd_certification_information") val wscdCertificationInformation: String,
    @SerialName("wscd_attack_resistance") val wscdAttackResistance: Int,
)

class InstantLongSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("InstantLongSerializer", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): kotlin.time.Instant {
        return kotlin.time.Instant.fromEpochMilliseconds(decoder.decodeLong())
    }

    override fun serialize(encoder: Encoder, value: kotlin.time.Instant) {
        encoder.encodeLong(value.toEpochMilliseconds())
    }
}