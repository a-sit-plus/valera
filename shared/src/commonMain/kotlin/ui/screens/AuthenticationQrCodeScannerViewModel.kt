package view

import at.asitplus.wallet.lib.jws.VerifierJwsService
import domain.BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase
import domain.RetrieveAuthenticationRequestParametersFromAuthenticationRequestUriUseCase
import domain.RetrieveRelyingPartyMetadataFromAuthenticationRequestParametersUseCase
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import ui.navigation.AuthenticationConsentPage

class AuthenticationQrCodeScannerViewModel(
    private val buildAuthenticationConsentPageFromAuthenticationRequestUriUseCase: BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase,
    private val retrieveRelyingPartyMetadataFromAuthenticationRequestParametersUseCase: RetrieveRelyingPartyMetadataFromAuthenticationRequestParametersUseCase,
) {
    constructor(
        client: HttpClient,
        verifierJwsService: VerifierJwsService,
    ) : this(
        buildAuthenticationConsentPageFromAuthenticationRequestUriUseCase = BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(
            retrieveAuthenticationRequestParametersFromAuthenticationRequestUriUseCase = RetrieveAuthenticationRequestParametersFromAuthenticationRequestUriUseCase(
                verifierJwsService = verifierJwsService,
                client = client,
            ),
        ),
        retrieveRelyingPartyMetadataFromAuthenticationRequestParametersUseCase = RetrieveRelyingPartyMetadataFromAuthenticationRequestParametersUseCase(
            verifierJwsService = verifierJwsService,
            client = client,
        ),
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
            val authenticationConsentPage =
                buildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(link).let {
                    AuthenticationConsentPage(
                        authenticationRequestParameters = it.authenticationRequestParameters,
                        recipientLocation = it.recipientLocation,
                        recipientName = it.recipientName,
                        fromQrCodeScanner = true,
                    )
                }
            val clientMetadataPayload =
                retrieveRelyingPartyMetadataFromAuthenticationRequestParametersUseCase(authenticationConsentPage.authenticationRequestParameters)

            clientMetadataPayload?.redirectUris?.run {
                if (!contains(authenticationConsentPage.authenticationRequestParameters.clientId)) {
                    val redirectUris = this.joinToString(", ") { "`${this}`" }
                    val message =
                        "Client id not in client metadata redirect uris: ${authenticationConsentPage.authenticationRequestParameters.clientId} not in: $redirectUris)"
                    throw Throwable(message)
                } else {
                    Napier.d("Valid client id: ${authenticationConsentPage.authenticationRequestParameters.clientId}")
                }
            } // ?: throw Throwable("No redirect URIs specified")

            stopLoadingCallback()
            onSuccess(authenticationConsentPage)
        }
    }
}