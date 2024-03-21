package domain

import at.asitplus.crypto.datatypes.jws.JwsSigned
import at.asitplus.wallet.lib.data.jsonSerializer
import at.asitplus.wallet.lib.jws.VerifierJwsService
import at.asitplus.wallet.lib.oidc.AuthenticationRequestParameters
import at.asitplus.wallet.lib.oidc.RelyingPartyMetadata
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class RetrieveRelyingPartyMetadataFromAuthenticationRequestParametersUseCase(
    private val client: HttpClient,
    private val verifierJwsService: VerifierJwsService,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend operator fun invoke(linkParams: AuthenticationRequestParameters): RelyingPartyMetadata? {
        return linkParams.clientMetadata ?: linkParams.clientMetadataUri?.let { metadataUri ->
            withContext(defaultDispatcher) {
                val metadataResponse = client.get(metadataUri)
                val metadataJws = try {
                    JwsSigned.parse(metadataResponse.bodyAsText()) ?: throw Exception()
                } catch (error: Throwable) {
                    throw Exception("Invalid metadataJws: ${metadataResponse.bodyAsText()}")
                }

                if (!verifierJwsService.verifyJwsObject(metadataJws)) {
                    throw Exception("Invalid metadataJws signature: ${metadataResponse.bodyAsText()}")
                }

                // metadata has been verified
                Napier.d("metadataJws payload: ${metadataJws.payload.decodeToString()}")
                jsonSerializer.decodeFromString<RelyingPartyMetadata>(metadataJws.payload.decodeToString())
            }
        }
    }
}