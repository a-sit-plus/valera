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
import io.ktor.http.HttpStatusCode
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

private class HttpHeader {
    companion object {
        val LOCATION = "location"
    }
}

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
            val pars = parseQueryString(link, startIndex = parameterIndex + 1)

            val requestUri = pars["request_uri"]
            if (requestUri == null) {
                walletMain.errorService.emit(
                    Exception(
                        pars["error_description"] ?: Resources.UNKNOWN_EXCEPTION
                    )
                )
                return@AuthenticationQrCodeScannerView
            }

            val metadata_uri = pars["metadata_uri"] ?: pars["client_metadata_uri"]
            if (metadata_uri == null) {
                walletMain.errorService.emit(
                    Exception(
                        pars["error_description"] ?: Resources.UNKNOWN_EXCEPTION
                    )
                )
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

                    val requestRedirectUri = requestResponse.headers[HttpHeader.LOCATION]
                    if (requestRedirectUri == null) {
                        walletMain.errorService.emit(
                            Exception("Missing redirect location: $requestResponse"),
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

                    val requestClientId = requestParams.getOrNull()?.clientId
                    val requestResponseRequestString = requestParams.getOrNull()?.request
                    if (requestResponseRequestString == null) {
                        walletMain.errorService.emit(
                            Exception("Missing Request Object: $requestRedirectUri"),
                        )
                        finalizeLoading()
                        return@launch
                    }

                    val requestResponseRequestJws = try {
                        JwsSigned.parse(requestResponseRequestString) ?: throw Exception()
                    } catch (error: Throwable) {
                        walletMain.errorService.emit(
                            Exception("Invalid requestResponseRequestJws: $requestResponseRequestString"),
                        )
                        finalizeLoading()
                        return@launch
                    }

                    if (
                        DefaultVerifierJwsService().verifyJwsObject(
                            requestResponseRequestJws,
                            null
                        ) == false
                    ) {
                        walletMain.errorService.emit(
                            Exception("Invalid requestResponseRequestJws Signature: $requestResponseRequestString"),
                        )
                        finalizeLoading()
                        return@launch
                    }

                    val requestParameters =
                        jsonSerializer.decodeFromString<AuthenticationRequestParameters>(
                            requestResponseRequestJws.payload.decodeToString()
                        )
                    if (requestParameters.clientId != requestClientId) {
                        walletMain.errorService.emit(
                            Exception("Client id does not match: UrlParameter: $requestClientId, AuthenticationRequestParameters: ${requestParameters.clientId}"),
                        )
                        finalizeLoading()
                        return@launch
                    }
                    RequestResponse(
                        redirectUri = requestRedirectUri,
                        requestParameters = requestParameters,
                    )
                }

                val clientMetadataPayload = null.let {
                    val metadataResponse = client.get(metadata_uri)
                    when (metadataResponse.status) {
                        HttpStatusCode.OK -> {
                            Napier.d("metadataResponse: ${metadataResponse.bodyAsText()}")
                        }

                        else -> {
                            walletMain.errorService.emit(
                                Exception("Invalid metadataResponse Status: ${metadataResponse.bodyAsText()}"),
                            )
                            finalizeLoading()
                            return@launch
                        }
                    }

                    val metadataJws = try {
                        JwsSigned.parse(metadataResponse.bodyAsText()) ?: throw Exception()
                    } catch (error: Throwable) {
                        walletMain.errorService.emit(
                            Exception("Invalid metadataJws: ${metadataResponse.bodyAsText()}"),
                        )
                        finalizeLoading()
                        return@launch
                    }

                    if (DefaultVerifierJwsService().verifyJwsObject(metadataJws, null) == false) {
                        walletMain.errorService.emit(
                            Exception("Invalid metadataJws Signature: ${metadataResponse.bodyAsText()}"),
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
                            "Client id not in client metadata redirect uris: ${authenticationRequestParameters.clientId} not in: \n${
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
                        recipientLocation = pars["client_id"] ?: "DemoLocation",
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