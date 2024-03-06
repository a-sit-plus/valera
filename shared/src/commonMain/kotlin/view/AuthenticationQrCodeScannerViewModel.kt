package view

import Resources
import at.asitplus.wallet.app.common.HttpService
import at.asitplus.wallet.lib.data.jsonSerializer
import at.asitplus.wallet.lib.jws.DefaultVerifierJwsService
import at.asitplus.wallet.lib.jws.JwsSigned
import at.asitplus.wallet.lib.oidc.AuthenticationRequestParameters
import at.asitplus.wallet.lib.oidc.RelyingPartyMetadata
import at.asitplus.wallet.lib.oidvci.decodeFromUrlQuery
import io.github.aakira.napier.Napier
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.parseQueryString
import io.ktor.util.flattenEntries
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import navigation.AuthenticationConsentPage

class AuthenticationQrCodeScannerViewModel {
    fun onScan(
        link: String,
        startLoadingCallback: () -> Unit,
        stopLoadingCallback: () -> Unit,
        onSuccess: (AuthenticationConsentPage) -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        startLoadingCallback()
        val coroutineExceptionHandler = CoroutineExceptionHandler { _, error ->
            // Do what you want with the error
            stopLoadingCallback()
            onFailure(error)
        }

        CoroutineScope(Dispatchers.IO).launch(coroutineExceptionHandler) {
            Napier.d("onScan: $link")

            val parameterIndex = link.indexOfFirst { it == '?' }
            val linkParams = parseQueryString(link, startIndex = parameterIndex + 1)

            val requestUri = linkParams["request_uri"]
                ?: throw Exception("${Resources.ERROR_QR_CODE_SCANNING_MISSING_REQUEST_URI}: $link")

            val metadataUri = linkParams["metadata_uri"] ?: linkParams["client_metadata_uri"]
            ?: throw Exception("${Resources.ERROR_QR_CODE_SCANNING_MISSING_CLIENT_METADATA}: $link")

            val client = HttpService().buildHttpClient()
            val verifierJwsService = DefaultVerifierJwsService()

            val (redirectUri, authenticationRequestParameters) = run {
                val requestResponse = client.get(requestUri)

                val requestRedirectUri =
                    requestResponse.headers[HttpHeaders.Location].also { Napier.d("Redirect location: $it") }
                        ?: throw Exception("${Resources.ERROR_QR_CODE_SCANNING_MISSING_REDIRECT_LOCATION}: $requestResponse")

                val requestParams = kotlin.runCatching {
                    Url(requestRedirectUri).parameters.flattenEntries().toMap()
                        .decodeFromUrlQuery<AuthenticationRequestParameters>()
                }

                val requestLocationClientId = requestParams.getOrNull()?.clientId
                val requestLocationRequestParameter = requestParams.getOrNull()?.request
                    ?: throw Exception("${Resources.ERROR_QR_CODE_SCANNING_MISSING_REQUEST_OBJECT_PARAMETER}: $requestRedirectUri")

                val requestLocationRequestParameterJws = try {
                    JwsSigned.parse(requestLocationRequestParameter) ?: throw Exception()
                } catch (error: Throwable) {
                    throw Exception("${Resources.ERROR_QR_CODE_SCANNING_INVALID_REQUEST_JWS_OBJECT}: $requestLocationRequestParameter")
                }

                if (!verifierJwsService.verifyJwsObject(requestLocationRequestParameterJws, null)) {
                    throw Exception("${Resources.ERROR_QR_CODE_SCANNING_INVALID_REQUEST_JWS_OBJECT_SIGNATURE}: $requestLocationRequestParameter")
                }

                val requestLocationRequestParameterParsed =
                    jsonSerializer.decodeFromString<AuthenticationRequestParameters>(
                        requestLocationRequestParameterJws.payload.decodeToString()
                    )
                if (requestLocationRequestParameterParsed.clientId != requestLocationClientId) {
                    throw Exception("${Resources.ERROR_QR_CODE_SCANNING_INCONSISTENT_CLIENT_ID}: UrlParameter: $requestLocationClientId, AuthenticationRequestParameters: ${requestLocationRequestParameterParsed.clientId}")
                }
                RequestResponse(
                    redirectUri = requestRedirectUri,
                    requestParameters = requestLocationRequestParameterParsed,
                )
            }

            val clientMetadataPayload = run {
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
                "client metadata redirect_uris:" +
                        " ${clientMetadataPayload.redirectUris.getOrElse(0) { "null" }}"
            )
            Napier.d("requested claims: ${requestedClaims.joinToString(", ")}")

            if (!clientMetadataPayload.redirectUris.contains(authenticationRequestParameters.clientId)) {
                val redirectUris = clientMetadataPayload.redirectUris.joinToString("\n - ")
                val message =
                    "${Resources.ERROR_QR_CODE_SCANNING_CLIENT_ID_NOT_IN_REDICECT_URIS}:" +
                            " ${authenticationRequestParameters.clientId} not in: \n$redirectUris)"
                throw Exception(message)
            } else {
                Napier.d("Valid client id: ${authenticationRequestParameters.clientId}")
            }

            stopLoadingCallback()
            // TODO("extract recipient name from the metadataResponse; the data is not yet being delivered though")
            onSuccess(
                AuthenticationConsentPage(
                    url = redirectUri,
                    claims = requestedClaims,
                    recipientName = "DemoService",
                    recipientLocation = linkParams["client_id"] ?: "DemoLocation",
                    fromQrCodeScanner = true,
                )
            )
        }
    }
}