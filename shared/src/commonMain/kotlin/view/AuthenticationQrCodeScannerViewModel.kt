package view

import Resources
import at.asitplus.wallet.lib.jws.VerifierJwsService
import domain.BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase
import domain.ExtractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase
import domain.RetrieveFinalAuthenticationRequestUriFromAuthenticationRequestUriUseCase
import domain.RetrieveRelyingPartyMetadataFromAuthenticationRequestUriUseCase
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import navigation.AuthenticationConsentPage

class AuthenticationQrCodeScannerViewModel(
    private val buildAuthenticationConsentPageFromAuthenticationRequestUriUseCase: BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase,
    private val retrieveRelyingPartyMetadataFromAuthenticationRequestUriUseCase: RetrieveRelyingPartyMetadataFromAuthenticationRequestUriUseCase,
    private val extractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase: ExtractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase,
) {
    constructor(
        client: HttpClient,
        verifierJwsService: VerifierJwsService,
    ) : this(
        client = client,
        verifierJwsService = verifierJwsService,
        extractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase = ExtractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase(
            verifierJwsService = verifierJwsService,
        )
    )

    constructor(
        client: HttpClient,
        verifierJwsService: VerifierJwsService,
        extractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase: ExtractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase,
    ) : this(
        buildAuthenticationConsentPageFromAuthenticationRequestUriUseCase = BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(
            extractAuthenticationRequestParametersFromAuthenticationRequestUri = extractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase,
            retrieveFinalAuthenticationRequestUriFromAuthenticationRequestUriUseCase = RetrieveFinalAuthenticationRequestUriFromAuthenticationRequestUriUseCase(
                extractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase = extractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase,
                client = client,
            )
        ),
        retrieveRelyingPartyMetadataFromAuthenticationRequestUriUseCase = RetrieveRelyingPartyMetadataFromAuthenticationRequestUriUseCase(
            extractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase = extractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase,
            client = client,
            verifierJwsService = verifierJwsService,
        ),
        extractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase = extractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase,
    )

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
                retrieveRelyingPartyMetadataFromAuthenticationRequestUriUseCase(link)
            val authenticationRequestParameters =
                extractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase(link)

            if (!clientMetadataPayload.redirectUris.contains(authenticationRequestParameters.clientId)) {
                val redirectUris = clientMetadataPayload.redirectUris.joinToString("\n - ")
                val message =
                    "${Resources.ERROR_QR_CODE_SCANNING_CLIENT_ID_NOT_IN_REDICECT_URIS}:" +
                            " ${authenticationRequestParameters.clientId} not in: \n$redirectUris)"
                throw Exception(message)
            } else {
                Napier.d("Valid client id: ${authenticationRequestParameters.clientId}")
            }

            val authenticationConsentPage =
                buildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(link).let {
                    AuthenticationConsentPage(
                        url = it.url,
                        claims = it.claims,
                        recipientLocation = it.recipientLocation,
                        recipientName = it.recipientName,
                        fromQrCodeScanner = true,
                    )
                }

            stopLoadingCallback()
            onSuccess(authenticationConsentPage)
        }
    }
}