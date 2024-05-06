package view

import at.asitplus.wallet.lib.data.jsonSerializer
import at.asitplus.wallet.lib.jws.VerifierJwsService
import at.asitplus.wallet.lib.oidc.AuthenticationRequestParameters
import at.asitplus.wallet.lib.oidc.OidcSiopWallet
import domain.BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase
import domain.RetrieveAuthenticationRequestParametersUseCase
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
        oidcSiopWallet: OidcSiopWallet,
    ) : this(
        buildAuthenticationConsentPageFromAuthenticationRequestUriUseCase = BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(
            retrieveAuthenticationRequestParametersUseCase = RetrieveAuthenticationRequestParametersUseCase(
                client = client,
                verifierJwsService = verifierJwsService,
                oidcSiopWallet = oidcSiopWallet,
            ),
        ),
        retrieveRelyingPartyMetadataFromAuthenticationRequestParametersUseCase = RetrieveRelyingPartyMetadataFromAuthenticationRequestParametersUseCase(
            client = client,
            verifierJwsService = verifierJwsService,
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
            stopLoadingCallback()
            onFailure(error)
        }

        CoroutineScope(Dispatchers.IO).launch(coroutineExceptionHandler) {
            val authenticationConsentPage =
                buildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(link).let {
                    AuthenticationConsentPage(
                        authenticationRequestParametersSerialized = it.authenticationRequestParametersSerialized,
                        recipientLocation = it.recipientLocation,
                        recipientName = it.recipientName,
                        fromQrCodeScanner = true,
                    )
                }
            val authenticationRequestParameters =
                jsonSerializer.decodeFromString<AuthenticationRequestParameters>(
                    authenticationConsentPage.authenticationRequestParametersSerialized
                )
            val clientMetadataPayload =
                retrieveRelyingPartyMetadataFromAuthenticationRequestParametersUseCase(
                    authenticationRequestParameters
                )

            authenticationRequestParameters.clientId?.let { clientId ->
                clientMetadataPayload?.redirectUris?.run {
                    if (!all {
                            it.substringAfter("//").substringBefore("/").endsWith(clientId)
                        }) {
                        val redirectUris = this.joinToString(", ") { "`${this}`" }
                        val message =
                            "Client id not in client metadata redirect uris: ${authenticationRequestParameters.clientId} not in: $redirectUris)"
                        throw Throwable(message)
                    } else {
                        Napier.d("Valid client id: ${authenticationRequestParameters.clientId}")
                    }
                }
            }

            stopLoadingCallback()
            onSuccess(authenticationConsentPage)
        }
    }
}