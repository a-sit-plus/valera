package domain

import Resources
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class InconsistentClientIdException(val uriBeforeRedirect: String, val uriAfterRedirect: String) : Exception(
    "${Resources.ERROR_INCONSISTENT_REDIRECT_URL_CLIENT_ID}: \n - ${uriBeforeRedirect}\n - ${uriAfterRedirect}"
)

class MissingHttpRedirectException(val response: HttpResponse) : Exception(
    "${Resources.ERROR_REQUEST_URI_REDIRECT_MISSING_LOCATION_HEADER}: $response"
)

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
                        throw Exception("${Resources.ERROR_REQUEST_URL_CONTAINING_REQUEST_OBJECT_AND_REQUEST_URI}: $location")
                    }

                    val requestResponse = client.get(requestUri)
                    requestResponse.headers[HttpHeaders.Location].also { Napier.d("Redirect location: $it") }
                        ?: throw MissingHttpRedirectException(requestResponse)
                } ?: throw Exception("Invalid while-loop iteration")

                val newAuthenticationRequestParameters =
                    extractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase(newLocation)

                if (authenticationRequestParameters.clientId != newAuthenticationRequestParameters.clientId) {
                    throw InconsistentClientIdException(location, newLocation)
                }

                location = newLocation
                authenticationRequestParameters = newAuthenticationRequestParameters
            }
            location
        }
    }
}