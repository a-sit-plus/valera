package at.asitplus.wallet.app.common.attestation

import at.asitplus.openid.DurationSecondsIntSerializer
import at.asitplus.signum.indispensable.josef.JsonWebKey
import at.asitplus.signum.indispensable.josef.JsonWebToken
import at.asitplus.signum.indispensable.josef.JwsSigned
import at.asitplus.signum.indispensable.josef.KeyAttestationJwt
import at.asitplus.signum.indispensable.josef.toJsonWebKey
import at.asitplus.wallet.app.common.WalletKeyMaterial
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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.collections.listOf
import kotlin.time.Duration

class KeyAttestationHelper(
    val host: Flow<String>,
    val httpClient: HttpClient,
    val keyMaterial: WalletKeyMaterial,
) {
    val keyAttestationEndpoint = host.map {
        URLBuilder(host.first()).apply {
            appendEncodedPathSegments(PATH_UNIT)
        }
    }

    suspend fun requestKeyAttestation(
        instanceAttestation: JwsSigned<JsonWebToken>,
        pop: JwsSigned<JsonWebToken>,
        nonce: String?,
        preferredKeyStorageStatusPeriod: Duration?,
        supportedAlgorithms: Collection<String>?

    ): JwsSigned<KeyAttestationJwt> {
        val holderKey = keyMaterial.getUnderLyingSigner()

        val response = httpClient.post(Url(keyAttestationEndpoint.first())) {
            contentType(ContentType.Application.Json)
            setBody(
                KeyAttestationRequest(
                    token = instanceAttestation.serialize(),
                    keys = listOf(holderKey.publicKey.toJsonWebKey()),
                    proof = pop.serialize(),
                    nonce = nonce,
                    keyStorage = setOf("iso_18045_moderate"),
                    userAuthentication = setOf("iso_18045_moderate"),
                    preferredKeyStorageStatusPeriod = preferredKeyStorageStatusPeriod,
                    supportedAlgorithms = supportedAlgorithms,
                )
            )
        }

        return JwsSigned.deserialize<KeyAttestationJwt>(
            it = response.bodyAsText(), deserializationStrategy = KeyAttestationJwt.serializer()
        ).getOrThrow()
    }

}

@Serializable
data class KeyAttestationRequest(
    @SerialName("token") val token: String,
    @SerialName("proof") val proof: String,
    @SerialName("keys") val keys: List<JsonWebKey>,
    @SerialName("nonce") val nonce: String?,
    @SerialName("key_storage") val keyStorage: Set<String>,
    @SerialName("user_authentication") val userAuthentication: Set<String>,
    @SerialName("preferred_key_storage_status_period")
    @Serializable(with = DurationSecondsIntSerializer::class)
    val preferredKeyStorageStatusPeriod: Duration?,
    @SerialName("supported_algorithms") val supportedAlgorithms: Collection<String>?,
)
