package at.asitplus.wallet.app.common

import at.asitplus.etsi.ListOfTrustedEntities
import at.asitplus.signum.indispensable.josef.JwsSigned
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.utils.io.InternalAPI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class TrustListService(
    private val httpService: HttpService
) {
    private val client = httpService.buildHttpClient()

    @OptIn(InternalAPI::class)
    suspend fun fetchTrustList(): ListOfTrustedEntities? {
        val url = "https://acceptance.trust.tech.ec.europa.eu/lists/eudiw/pid-providers.json"

        val response = client.get(url) {
            accept(ContentType.Application.Json)
        }

        val jws = JwsSigned.deserialize(
            TrustListJwsPayload.serializer(),
            response.body<String>(),
            joseCompliantSerializer
        ).getOrThrow()

        val ret = jws.payload.loTe
        return ret
    }
}

@Serializable
data class TrustListJwsPayload(
    @SerialName("LoTE")
    val loTe: ListOfTrustedEntities
)