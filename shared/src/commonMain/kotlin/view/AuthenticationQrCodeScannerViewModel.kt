package view

import at.asitplus.wallet.lib.jws.VerifierJwsService
import domain.BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase
import domain.ExtractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase
import domain.ExtractClaimsFromPresentationDefinitionUseCase
import domain.RetrieveFinalAuthenticationRequestUriFromAuthenticationRequestUriUseCase
import domain.RetrieveRelyingPartyMetadataFromAuthenticationRequestUriUseCase
import domain.ValidateClientIdConsistencyWithClientMetadataUseCase
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
    private val validateClientIdConsistencyWithClientMetadataUseCase: ValidateClientIdConsistencyWithClientMetadataUseCase,
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
            extractClaimsFromPresentationDefinitionUseCase = ExtractClaimsFromPresentationDefinitionUseCase(),
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
        validateClientIdConsistencyWithClientMetadataUseCase = ValidateClientIdConsistencyWithClientMetadataUseCase(),
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
            val authenticationRequestParameters = extractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase(link)
            validateClientIdConsistencyWithClientMetadataUseCase(
                clientMetadataPayload = clientMetadataPayload,
                clientId = authenticationRequestParameters.clientId,
            )

            val authenticationConsentPage =
                buildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(link)

            stopLoadingCallback()
            onSuccess(authenticationConsentPage)
        }
    }
}