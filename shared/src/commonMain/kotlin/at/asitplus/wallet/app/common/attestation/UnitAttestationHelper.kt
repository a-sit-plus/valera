package at.asitplus.wallet.app.common.attestation

import at.asitplus.openid.DurationSecondsIntSerializer
import at.asitplus.signum.indispensable.josef.JsonWebKey
import at.asitplus.signum.indispensable.josef.JsonWebToken
import at.asitplus.signum.indispensable.josef.JwsSigned
import at.asitplus.signum.indispensable.josef.KeyAttestationJwt
import at.asitplus.signum.indispensable.josef.toJsonWebKey
import at.asitplus.wallet.app.common.HttpService
import at.asitplus.wallet.app.common.WalletKeyMaterial
import at.asitplus.wallet.lib.oidvci.WalletService.KeyAttestationInput
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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration

class UnitAttestationHelper(
    val host: Flow<String>,
    httpService: HttpService,
    val keyMaterial: WalletKeyMaterial,
) {
    val unitEndpoint = host.map {
        URLBuilder(host.first()).apply {
            appendEncodedPathSegments(PATH_UNIT)
        }
    }
    val httpClient = httpService.buildHttpClient()

    suspend fun requestUnitAttestation(
        instanceAttestation: JwsSigned<JsonWebToken>,
        pop: JwsSigned<JsonWebToken>,
        input: KeyAttestationInput,
    ): JwsSigned<KeyAttestationJwt> {
        val holderKey = keyMaterial.getUnderLyingSigner()

        val response = httpClient.post(Url(unitEndpoint.first())) {
            contentType(ContentType.Application.Json)
            setBody(
                UnitAttestationRequest(
                    token = instanceAttestation.serialize(),
                    keys = listOf(holderKey.publicKey.toJsonWebKey()),
                    proof = pop.serialize(),
                    nonce = input.clientNonce,
                    credentialIssuer = input.credentialIssuer,
                    preferredKeyStorageStatusPeriod = input.preferredKeyStorageStatusPeriod,
                    supportedAlgorithms = input.supportedAlgorithms,
                )
            )
        }

        return JwsSigned.deserialize<KeyAttestationJwt>(
            it = response.bodyAsText(), deserializationStrategy = KeyAttestationJwt.serializer()
        ).getOrThrow()
    }

}

@Serializable
data class UnitAttestationRequest(
    @SerialName("token") val token: String,
    @SerialName("proof") val proof: String,
    @SerialName("keys") val keys: List<JsonWebKey>,
    @SerialName("nonce") val nonce: String?,
    @SerialName("credential_issuer") val credentialIssuer: String?,
    @SerialName("preferred_key_storage_status_period")
    @Serializable(with = DurationSecondsIntSerializer::class)
    val preferredKeyStorageStatusPeriod: Duration?,
    @SerialName("supported_algorithms") val supportedAlgorithms: Collection<String>?,
)
