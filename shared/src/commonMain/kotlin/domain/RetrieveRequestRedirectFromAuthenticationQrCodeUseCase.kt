package domain

import Resources
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.parseQueryString
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext


class RetrieveRequestRedirectFromAuthenticationQrCodeUseCase(
    private val client: HttpClient,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend operator fun invoke(link: String): String {
        return withContext(defaultDispatcher) {
            val parameterIndex = link.indexOfFirst { it == '?' }
            val linkParams = parseQueryString(link, startIndex = parameterIndex + 1)

            val requestUri = linkParams["request_uri"]
                ?: throw Exception("${Resources.ERROR_QR_CODE_SCANNING_MISSING_REQUEST_URI}: $link")

            val requestResponse = client.get(requestUri)
            requestResponse.headers[HttpHeaders.Location].also { Napier.d("Redirect location: $it") }
                ?: throw Exception("${Resources.ERROR_QR_CODE_SCANNING_MISSING_REDIRECT_LOCATION}: $requestResponse")
        }
    }
}