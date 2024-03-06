package view

import Resources
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.data.jsonSerializer
import at.asitplus.wallet.lib.jws.DefaultVerifierJwsService
import at.asitplus.wallet.lib.jws.JwsSigned
import at.asitplus.wallet.lib.oidc.AuthenticationRequestParameters
import at.asitplus.wallet.lib.oidc.RelyingPartyMetadata
import at.asitplus.wallet.lib.oidvci.decodeFromUrlQuery
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.parseQueryString
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.flattenEntries
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import navigation.AuthenticationConsentPage
import ui.composables.buttons.NavigateUpButton
import ui.views.CameraView

data class RequestResponse(
    val redirectUri: String,
    val requestParameters: AuthenticationRequestParameters,
)

@Composable
fun AuthenticationQrCodeScannerScreen(
    navigateUp: () -> Unit,
    navigateToLoadingScreen: () -> Unit,
    navigateToConsentScreen: (AuthenticationConsentPage) -> Unit,
    walletMain: WalletMain,
) {
    AuthenticationQrCodeScannerView(
        navigateUp = navigateUp,
        onFoundPayload = { link ->
            Napier.d("onScan: $link")

            val parameterIndex = link.indexOfFirst { it == '?' }
            val requestParams = parseQueryString(link, startIndex = parameterIndex + 1)

            val errorDescription = requestParams["error_description"] ?: Resources.UNKNOWN_EXCEPTION
            val requestUri = requestParams["request_uri"]
            if (requestUri == null) {
                walletMain.errorService.emit(Exception(errorDescription))
                return@AuthenticationQrCodeScannerView
            }

            val metadata_uri = requestParams["metadata_uri"] ?: requestParams["client_metadata_uri"]
            if (metadata_uri == null) {
                walletMain.errorService.emit(Exception(errorDescription))
                return@AuthenticationQrCodeScannerView
            }

            navigateToLoadingScreen()
            fun finalizeLoading() {
                navigateUp()
            }

            val client = HttpClient {
                followRedirects = false
                install(ContentNegotiation) {
                    json()
                }

                install(DefaultRequest) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                }

                install(Logging) {
                    logger = Logger.DEFAULT
                    level = LogLevel.ALL
                }
            }

            CoroutineScope(Dispatchers.IO).launch {
                val (redirectUri, authenticationRequestParameters) = null.let {
                    val requestResponse = client.get(requestUri)

                    val requestRedirectUri = requestResponse.headers[HttpHeaders.Location]
                    if (requestRedirectUri == null) {
                        walletMain.errorService.emit(
                            Exception("${Resources.ERROR_QR_CODE_SCANNING_MISSING_REDIRECT_LOCATION}: $requestResponse"),
                        )
                        finalizeLoading()
                        return@launch
                    } else {
                        Napier.d("Redirect location: $requestRedirectUri")
                    }

                    val requestParams = kotlin.runCatching {
                        Url(requestRedirectUri).parameters.flattenEntries().toMap()
                            .decodeFromUrlQuery<AuthenticationRequestParameters>()
                    }

                    val requestLocationClientId = requestParams.getOrNull()?.clientId
                    val requestLocationRequestParameter = requestParams.getOrNull()?.request
                    if (requestLocationRequestParameter == null) {
                        walletMain.errorService.emit(
                            Exception("${Resources.ERROR_QR_CODE_SCANNING_MISSING_REQUEST_OBJECT_PARAMETER}: $requestRedirectUri"),
                        )
                        finalizeLoading()
                        return@launch
                    }

                    val requestLocationRequestParameterJws = try {
                        JwsSigned.parse(requestLocationRequestParameter) ?: throw Exception()
                    } catch (error: Throwable) {
                        walletMain.errorService.emit(
                            Exception("${Resources.ERROR_QR_CODE_SCANNING_INVALID_REQUEST_JWS_OBJECT}: $requestLocationRequestParameter"),
                        )
                        finalizeLoading()
                        return@launch
                    }

                    if (!DefaultVerifierJwsService().verifyJwsObject(requestLocationRequestParameterJws, null)) {
                        walletMain.errorService.emit(
                            Exception("${Resources.ERROR_QR_CODE_SCANNING_INVALID_REQUEST_JWS_OBJECT_SIGNATURE}: $requestLocationRequestParameter"),
                        )
                        finalizeLoading()
                        return@launch
                    }

                    val requestLocationRequestParameterParsed =
                        jsonSerializer.decodeFromString<AuthenticationRequestParameters>(
                            requestLocationRequestParameterJws.payload.decodeToString()
                        )
                    if (requestLocationRequestParameterParsed.clientId != requestLocationClientId) {
                        walletMain.errorService.emit(
                            Exception("${Resources.ERROR_QR_CODE_SCANNING_INCONSISTENT_CLIENT_ID}: UrlParameter: $requestLocationClientId, AuthenticationRequestParameters: ${requestLocationRequestParameterParsed.clientId}"),
                        )
                        finalizeLoading()
                        return@launch
                    }
                    RequestResponse(
                        redirectUri = requestRedirectUri,
                        requestParameters = requestLocationRequestParameterParsed,
                    )
                }

                val clientMetadataPayload = null.let {
                    val metadataResponse = client.get(metadata_uri)

                    val metadataJws = try {
                        JwsSigned.parse(metadataResponse.bodyAsText()) ?: throw Exception()
                    } catch (error: Throwable) {
                        walletMain.errorService.emit(
                            Exception("${Resources.ERROR_QR_CODE_SCANNING_INVALID_METADATA_JWS_OBJECT}: ${metadataResponse.bodyAsText()}"),
                        )
                        finalizeLoading()
                        return@launch
                    }

                    if (DefaultVerifierJwsService().verifyJwsObject(metadataJws, null) == false) {
                        walletMain.errorService.emit(
                            Exception("${Resources.ERROR_QR_CODE_SCANNING_INVALID_METADATA_JWS_OBJECT_SIGNATURE}: ${metadataResponse.bodyAsText()}"),
                        )
                        finalizeLoading()
                        return@launch
                    }

                    // metadata has been verified
                    Napier.d("metadataJws payload: ${metadataJws.payload.decodeToString()}")
                    jsonSerializer.decodeFromString<RelyingPartyMetadata>(metadataJws.payload.decodeToString())
                }

                val requestedClaims =
                    authenticationRequestParameters.presentationDefinition?.inputDescriptors
                        ?.mapNotNull { it.constraints }?.flatMap { it.fields?.toList() ?: listOf() }
                        ?.flatMap { it.path.toList() }
                        ?.filter { it != "$.type" }
                        ?.filter { it != "$.mdoc.doctype" }
                        ?.map { it.removePrefix("\$.mdoc.") }
                        ?.map { it.removePrefix("\$.") }
                        ?: listOf()

                Napier.d("redirectUri: $redirectUri")
                Napier.d("clientId: ${authenticationRequestParameters.clientId}")
                Napier.d(
                    "client metadata redirect_uris: ${
                        clientMetadataPayload.redirectUris.getOrElse(
                            0
                        ) { "null" }
                    }"
                )

                Napier.d("requested claims: ${requestedClaims.joinToString(", ")}")

                if (clientMetadataPayload.redirectUris.contains(authenticationRequestParameters.clientId) == false) {
                    walletMain.errorService.emit(
                        Exception(
                            "${Resources.ERROR_QR_CODE_SCANNING_CLIENT_ID_NOT_IN_REDICECT_URIS}: ${authenticationRequestParameters.clientId} not in: \n${
                                clientMetadataPayload.redirectUris.joinToString(
                                    "\n - "
                                )
                            })"
                        ),
                    )
                    finalizeLoading()
                    return@launch
                } else {
                    Napier.d("Valid client id: ${authenticationRequestParameters.clientId}")
                }

                finalizeLoading()
                navigateUp()
                // TODO("extract recipient name from the metadataResponse; the data is not yet being delivered though")
                navigateToConsentScreen(
                    AuthenticationConsentPage(
                        url = redirectUri,
                        claims = requestedClaims,
                        recipientName = "DemoService",
                        recipientLocation = requestParams["client_id"] ?: "DemoLocation",
                        fromQrCodeScanner = true,
                    )
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticationQrCodeScannerView(
    navigateUp: () -> Unit,
    onFoundPayload: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            Resources.HEADING_LABEL_AUTHENTICATE_AT_DEVICE_TITLE,
                            style = MaterialTheme.typography.headlineLarge,
                        )
                        Text(
                            Resources.HEADING_LABEL_AUTHENTICATE_AT_DEVICE_SUBTITLE,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                },
                navigationIcon = {
                    NavigateUpButton(navigateUp)
                },
            )
        },
    ) {
        Column(modifier = Modifier.padding(it).fillMaxSize()) {
            CameraView(
                onFoundPayload = onFoundPayload,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}