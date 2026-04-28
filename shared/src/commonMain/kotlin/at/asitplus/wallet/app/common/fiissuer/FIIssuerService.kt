package at.asitplus.wallet.app.common.fiissuer

import at.asitplus.wallet.app.common.HttpService
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.encodeURLPath
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class FIIssuerCredentialTypeNamesResponseDto(
    val credentialTypes: List<String>,
)

@Serializable
data class FIIssuerCredentialTypeDto(
    val typeName: String,
    val requiredClaimKeys: List<String>,
)

@Serializable
data class FIIssuerCreateCredentialRequestDto(
    val credentialType: String,
    val claims: Map<String, String>,
    val proof: String? = null,
)

@Serializable
data class FIIssuerCredentialRequestResponseDto(
    val id: Int,
    val transactionId: String,
    val status: FIIssuerCredentialRequestStatus,
)

@Serializable
data class FIIssuerCredentialOfferDto(
    val transactionId: String,
    val status: FIIssuerCredentialRequestStatus,
    val credential: String? = null,
)

@Serializable
enum class FIIssuerCredentialRequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    EXPIRED,
}

class FIIssuerService(
    private val httpService: HttpService,
) {
    private val baseUrl = "https://fiissuer.azurewebsites.net"

    suspend fun listCredentialTypeNames(): List<String> = httpService.buildHttpClient().use { client ->
        client.get("$baseUrl/api/credentials/types/names").body<FIIssuerCredentialTypeNamesResponseDto>().credentialTypes
    }

    suspend fun getCredentialType(typeName: String): FIIssuerCredentialTypeDto = httpService.buildHttpClient().use { client ->
        client.get("$baseUrl/api/credentials/types/${typeName.encodeURLPath()}").body()
    }

    suspend fun createCredentialRequest(
        credentialType: String,
        claims: Map<String, String>,
        proof: String? = null,
    ): FIIssuerCredentialRequestResponseDto = httpService.buildHttpClient().use { client ->
        client.post("$baseUrl/api/credentials/requests") {
            setBody(
                buildJsonObject {
                    put("credentialType", credentialType)
                    put(
                        "claims",
                        buildJsonObject {
                            claims.forEach { (key, value) ->
                                put(key, value)
                            }
                        }
                    )
                    proof?.takeIf { it.isNotBlank() }?.let { put("proof", it) }
                }
            )
        }.body()
    }

    suspend fun getCredentialOffer(transactionId: String): FIIssuerCredentialOfferDto = httpService.buildHttpClient().use { client ->
        client.get("$baseUrl/api/credentials/offers/${transactionId.encodeURLPath()}").body()
    }
}

