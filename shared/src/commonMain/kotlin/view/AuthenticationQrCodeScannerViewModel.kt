package view

import Resources
import domain.ExtractClaimsFromPresentationDefinitionUseCase
import domain.ExtractRequestObjectFromRedirectUriUseCase
import domain.RetrieveRelyingPartyMetadataFromAuthenticationQrCodeUseCase
import domain.RetrieveRequestRedirectFromAuthenticationQrCodeUseCase
import domain.ValidateClientMetadataAndRequestParameterConsistencyUseCase
import io.github.aakira.napier.Napier
import io.ktor.http.parseQueryString
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import navigation.AuthenticationConsentPage

class AuthenticationQrCodeScannerViewModel(
    private val retrieveRelyingPartyMetadataFromAuthenticationQrCodeUseCase: RetrieveRelyingPartyMetadataFromAuthenticationQrCodeUseCase,
    private val retrieveRequestRedirectFromAuthenticationQrCodeUseCase: RetrieveRequestRedirectFromAuthenticationQrCodeUseCase,
    private val extractRequestObjectFromRedirectUriUseCase: ExtractRequestObjectFromRedirectUriUseCase = ExtractRequestObjectFromRedirectUriUseCase(),
    private val validateClientMetadataAndRequestParameterConsistencyUseCase: ValidateClientMetadataAndRequestParameterConsistencyUseCase = ValidateClientMetadataAndRequestParameterConsistencyUseCase(),
    private val extractClaimsFromPresentationDefinitionUseCase: ExtractClaimsFromPresentationDefinitionUseCase = ExtractClaimsFromPresentationDefinitionUseCase(),
) {
    fun onScan(
        link: String,
        startLoadingCallback: () -> Unit,
        stopLoadingCallback: () -> Unit,
        onSuccess: (AuthenticationConsentPage) -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        Napier.d("onScan: $link")

        startLoadingCallback()
        val coroutineExceptionHandler = CoroutineExceptionHandler { _, error ->
            // Do what you want with the error
            stopLoadingCallback()
            onFailure(error)
        }

        CoroutineScope(Dispatchers.IO).launch(coroutineExceptionHandler) {
            val clientMetadataPayload =
                retrieveRelyingPartyMetadataFromAuthenticationQrCodeUseCase(link)
            val redirectUri = retrieveRequestRedirectFromAuthenticationQrCodeUseCase(link)
            val authenticationRequestParameters =
                extractRequestObjectFromRedirectUriUseCase(redirectUri)
            validateClientMetadataAndRequestParameterConsistencyUseCase(
                clientMetadataPayload = clientMetadataPayload,
                authenticationRequestParameters = authenticationRequestParameters,
            )

            val parameterIndex = link.indexOfFirst { it == '?' }
            val linkParams = parseQueryString(link, startIndex = parameterIndex + 1)
            val scannedLinkClientId = linkParams["client_id"]?.also {
                if (it != authenticationRequestParameters.clientId) {
                    throw Exception("${Resources.ERROR_QR_CODE_SCANNING_INCONSISTENT_CLIENT_ID}: ScannedLink: $it; RequestObject: ${authenticationRequestParameters.clientId}")
                }
            }

            val requestedClaims = authenticationRequestParameters.presentationDefinition?.let {
                extractClaimsFromPresentationDefinitionUseCase(it)
            } ?: listOf()

            Napier.d("redirectUri: $redirectUri")
            Napier.d("clientId: ${authenticationRequestParameters.clientId}")
            Napier.d(
                "client metadata redirect_uris:" +
                        " ${clientMetadataPayload.redirectUris.getOrElse(0) { "null" }}"
            )
            Napier.d("requested claims: ${requestedClaims.joinToString(", ")}")

            stopLoadingCallback()
            // TODO("extract recipient name from the metadataResponse; the data is not yet being delivered though")
            onSuccess(
                AuthenticationConsentPage(
                    url = redirectUri,
                    claims = requestedClaims,
                    recipientName = "DemoService",
                    recipientLocation = scannedLinkClientId ?: "DemoLocation",
                    fromQrCodeScanner = true,
                )
            )
        }
    }
}