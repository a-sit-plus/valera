package at.asitplus.wallet.app.common.attestation

import at.asitplus.signum.indispensable.josef.JsonWebKey
import at.asitplus.signum.indispensable.josef.JsonWebToken
import at.asitplus.signum.indispensable.josef.JwsHeader
import at.asitplus.signum.indispensable.josef.JwsSigned
import at.asitplus.signum.indispensable.josef.KeyAttestationJwt
import at.asitplus.signum.indispensable.josef.toJsonWebKey
import at.asitplus.wallet.app.common.HttpService
import at.asitplus.wallet.app.common.WalletKeyMaterial
import at.asitplus.wallet.lib.agent.KeyMaterial
import at.asitplus.wallet.lib.jws.JwsHeaderIdentifierFun
import at.asitplus.wallet.lib.jws.SignJwt
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
        pop: JwsSigned<JsonWebToken>
    ): JwsSigned<KeyAttestationJwt> {
        val holderKey = keyMaterial.getUnderLyingSigner()

        val response = httpClient.post(Url(unitEndpoint.first())) {
            contentType(ContentType.Application.Json)
            setBody(
                UnitAttestationRequest(
                    token = instanceAttestation.serialize(),
                    keys = listOf(holderKey.publicKey.toJsonWebKey()),
                    storageType = LOCAL_NATIVE,
                    proof = pop.serialize()
                )
            )
        }

        return JwsSigned.deserialize<KeyAttestationJwt>(
            it = response.bodyAsText(), deserializationStrategy = KeyAttestationJwt.serializer()
        ).getOrThrow()
    }

    suspend fun buildProofOfPossession(
        unitAttestation: JwsSigned<KeyAttestationJwt>,
        type: String,
        payload: JsonWebToken
    ) =
        keyMaterial.getUnderLyingSigner().let {
            SignJwt<JsonWebToken>(
                keyMaterial, JwsHeaderUnitAttestationPop(unitAttestation.serialize())
            ).invoke(
                type,
                payload,
                JsonWebToken.serializer(),
            ).getOrThrow()
        }
}

@Serializable
data class UnitAttestationRequest(
    @SerialName("token") val token: String,
    @SerialName("proof") val proof: String,
    @SerialName("keys") val keys: List<JsonWebKey>,
    @SerialName("storage_type") val storageType: String,
)

/**
 * JwsHeader for UnitAttestationPop
 * kid value hardcoded to 0 see:
 * https://github.com/eu-digital-identity-wallet/eudi-doc-standards-and-technical-specifications/blob/main/docs/technical-specifications/ts3-wallet-unit-attestation.md
 */
class JwsHeaderUnitAttestationPop(val wua: String) : JwsHeaderIdentifierFun {
    override suspend operator fun invoke(
        it: JwsHeader,
        keyMaterial: KeyMaterial,
    ) = it.copy(keyId = "0", keyAttestation = wua, jsonWebKey = keyMaterial.jsonWebKey)
}