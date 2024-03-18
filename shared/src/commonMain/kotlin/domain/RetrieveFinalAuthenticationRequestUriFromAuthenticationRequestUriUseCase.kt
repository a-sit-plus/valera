package domain

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
class RetrieveFinalAuthenticationRequestUriFromAuthenticationRequestUriUseCase(
    private val extractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase: ExtractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase,
    private val client: HttpClient,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend operator fun invoke(link: String): String {
        return withContext(defaultDispatcher) {
            var location = link
            var authenticationRequestParameters =
                extractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase(location)
            while (authenticationRequestParameters.requestUri != null) {
                val newLocation = authenticationRequestParameters.requestUri?.let { requestUri ->
                    if (authenticationRequestParameters.request != null) {
                        throw Exception("Invalid request url: contains both parameters 'request' and 'request_uri': $location")
                    }

                    val requestResponse = client.get(requestUri)
                    requestResponse.headers[HttpHeaders.Location].also { Napier.d("Redirect location: $it") }
                        ?: throw Exception("Missing location response header: $requestResponse")
                } ?: throw Exception("Invalid while-loop iteration")

                val newAuthenticationRequestParameters =
                    extractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase(newLocation)

                if (authenticationRequestParameters.clientId != newAuthenticationRequestParameters.clientId) {
                    throw Exception("Authentication failed: '${authenticationRequestParameters.clientId}' vs '${newAuthenticationRequestParameters.clientId}'")
                }

                location = newLocation
                authenticationRequestParameters = newAuthenticationRequestParameters
            }
            location
        }
    }
}