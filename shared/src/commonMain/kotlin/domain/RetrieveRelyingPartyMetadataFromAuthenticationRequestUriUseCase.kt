package domain

import Resources
import at.asitplus.wallet.lib.data.jsonSerializer
import at.asitplus.wallet.lib.jws.JwsSigned
import at.asitplus.wallet.lib.jws.VerifierJwsService
import at.asitplus.wallet.lib.oidc.RelyingPartyMetadata
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class RetrieveRelyingPartyMetadataFromAuthenticationRequestUriUseCase(
    private val extractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase: ExtractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase,
    private val client: HttpClient,
    private val verifierJwsService: VerifierJwsService,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend operator fun invoke(link: String): RelyingPartyMetadata {
        return extractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase(link).let { linkParams ->
            linkParams.clientMetadata ?: linkParams.clientMetadataUri?.let { metadataUri ->
                withContext(defaultDispatcher) {
                    val metadataResponse = client.get(metadataUri)
                    val metadataJws = try {
                        JwsSigned.parse(metadataResponse.bodyAsText()) ?: throw Exception()
                    } catch (error: Throwable) {
                        throw Exception("${Resources.ERROR_QR_CODE_SCANNING_INVALID_METADATA_JWS_OBJECT}: ${metadataResponse.bodyAsText()}")
                    }

                    if (!verifierJwsService.verifyJwsObject(metadataJws, null)) {
                        throw Exception("${Resources.ERROR_QR_CODE_SCANNING_INVALID_METADATA_JWS_OBJECT_SIGNATURE}: ${metadataResponse.bodyAsText()}")
                    }

                    // metadata has been verified
                    Napier.d("metadataJws payload: ${metadataJws.payload.decodeToString()}")
                    jsonSerializer.decodeFromString<RelyingPartyMetadata>(metadataJws.payload.decodeToString())
                }
            }
        } ?: throw Exception("${Resources.ERROR_QR_CODE_SCANNING_MISSING_CLIENT_METADATA}: $link")
    }
}