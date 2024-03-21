package domain

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class RetrieveFinalAuthenticationRequestUriFromAuthenticationRequestUriUseCase(
    private val extractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase: ExtractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase,
    private val client: HttpClient,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend operator fun invoke(link: String): String {
        return withContext(defaultDispatcher) {
            var location = link
            var authenticationRequestParameters =
                extractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase(location).also {
                    Napier.d("requestUri: ${it.requestUri}")
                }
            while (authenticationRequestParameters.requestUri != null) {
                val requestUri = authenticationRequestParameters.requestUri
                    ?: throw Exception("Invalid while-loop iteration")

                val newLocation = run {
                    if (authenticationRequestParameters.request != null) {
                        throw Exception("Invalid request url: contains both parameters 'request' and 'request_uri': $location")
                    }

                    val requestResponse = client.get(requestUri)
                    requestResponse.headers[HttpHeaders.Location].also { Napier.d("Redirect location: $it") }
                        ?: requestUri // keep request uri if no further redirect is found
                }

                val newAuthenticationRequestParameters =
                    extractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase(
                        newLocation
                    )

                if (authenticationRequestParameters.clientId != newAuthenticationRequestParameters.clientId) {
                    throw Exception("Inconsistent client ids: '${authenticationRequestParameters.clientId}' vs '${newAuthenticationRequestParameters.clientId}'")
                }

                location = newLocation
                authenticationRequestParameters = newAuthenticationRequestParameters
            }
            location
        }
    }
}