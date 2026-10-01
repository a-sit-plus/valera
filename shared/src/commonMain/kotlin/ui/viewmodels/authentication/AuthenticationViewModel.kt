package ui.viewmodels.authentication

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.catchingUnwrapped
import at.asitplus.openid.TransactionDataBase64Url
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.biometric_authentication_prompt_for_data_transmission_consent_title
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.agent.CredentialMatchingResult
import at.asitplus.wallet.lib.agent.IsoDeviceRetrievalMatchingResult
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.CredentialPresentation
import at.asitplus.wallet.lib.data.CredentialPresentation.*
import at.asitplus.wallet.lib.data.CredentialPresentationRequest
import at.asitplus.wallet.lib.ktor.openid.OpenId4VpWallet
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import ui.presentation.hasUnsatisfiedDocumentRequest

abstract class AuthenticationViewModel(
    val spName: String?,
    val spLocation: String,
    val spImage: ImageBitmap?,
    val navigateUp: () -> Unit,
    val onAuthenticationSuccess: (redirectUrl: String?) -> Unit,
    val navigateToHomeScreen: () -> Unit,
    val walletMain: WalletMain,
    val onClickLogo: () -> Unit
) {
    abstract val presentationRequest: CredentialPresentationRequest

    var viewState by mutableStateOf(AuthenticationViewState.Consent)
    abstract val transactionData: TransactionDataBase64Url?

    lateinit var matchingCredentials: CredentialMatchingResult<SubjectCredentialStore.StoreEntry>

    abstract suspend fun findMatchingCredentials(): Result<CredentialMatchingResult<SubjectCredentialStore.StoreEntry>>

    open fun onCancel() {
        navigateUp()
    }

    open fun onError(error: Throwable) {
        walletMain.errorService.emit(error)
    }

    suspend fun onConsent() {
        matchingCredentials = findMatchingCredentials().getOrElse {
            viewState = AuthenticationViewState.NoMatchingCredential
            Napier.w("No matching credential", it)
            return
        }

        when (val matching = matchingCredentials) {
            is at.asitplus.wallet.lib.agent.DCQLMatchingResult -> {
                matching.matchingResult.toDefaultSubmission(matching.presentationRequest.dcqlQuery)
                // TODO: create default selection?
                // matching fails if query is not satisfiable, so we know that selection is the next step
                viewState = AuthenticationViewState.Selection
            }

            is IsoDeviceRetrievalMatchingResult -> viewState =
                if (matching.matchingResult.hasUnsatisfiedDocumentRequest()) {
                    AuthenticationViewState.NoMatchingCredential
                } else {
                    AuthenticationViewState.Selection
                }

            else -> throw UnsupportedOperationException(
                "Unsupported credential matching result: ${matching::class.simpleName}"
            )
        }
    }

    fun confirmSelection(credentialPresentationSubmissions: CredentialPresentationSubmissions<SubjectCredentialStore.StoreEntry>?) {
        walletMain.scope.launch {
            finalizeAuthorization(
                when(credentialPresentationSubmissions) {
                    is DCQLCredentialSubmissions -> CredentialPresentation.DCQLPresentation(
                        presentationRequest = presentationRequest as CredentialPresentationRequest.DCQLRequest,
                        credentialQuerySubmissions = credentialPresentationSubmissions.credentialQuerySubmissions
                    )

                    is IsoDeviceRequestCredentialSubmissions -> IsoDeviceRetrievalPresentation(
                        presentationRequest = presentationRequest as CredentialPresentationRequest.IsoDeviceRetrieval,
                        submissions = credentialPresentationSubmissions.submissions,
                    )

                    null -> when(val it = presentationRequest) {
                        is CredentialPresentationRequest.DCQLRequest -> DCQLPresentation(
                            presentationRequest = it,
                            credentialQuerySubmissions = null
                        )

                        is CredentialPresentationRequest.IsoDeviceRetrieval -> IsoDeviceRetrievalPresentation(
                            presentationRequest = it,
                            submissions = null,
                        )

                        else -> throw UnsupportedOperationException(
                            "Unsupported presentation request: ${it::class.simpleName}"
                        )
                    }
                }
            )
        }
    }


    abstract suspend fun finalizationMethod(credentialPresentation: CredentialPresentation): OpenId4VpWallet.AuthenticationResult

    protected open fun handleAuthenticationSuccess(result: OpenId4VpWallet.AuthenticationSuccess) {
        navigateUp()
        onAuthenticationSuccess(result.redirectUri)
    }

    private suspend fun finalizeAuthorization(credentialPresentation: CredentialPresentation) {
        catchingUnwrapped {
            walletMain.keyMaterial.promptText =
                getString(Res.string.biometric_authentication_prompt_for_data_transmission_consent_title)
            finalizationMethod(credentialPresentation) as OpenId4VpWallet.AuthenticationSuccess
        }.onSuccess {
            handleAuthenticationSuccess(it)
        }.onFailure {
            onError(it)
        }
    }
}

enum class AuthenticationViewState {
    Consent,
    NoMatchingCredential,
    Selection
}
