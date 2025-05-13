package ui.viewmodels.authentication

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.KmmResult
import at.asitplus.catchingUnwrapped
import at.asitplus.openid.TransactionDataBase64Url
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.biometric_authentication_prompt_for_data_transmission_consent_subtitle
import at.asitplus.valera.resources.biometric_authentication_prompt_for_data_transmission_consent_title
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.CredentialPresentation
import at.asitplus.wallet.lib.data.CredentialPresentationRequest
import at.asitplus.wallet.lib.data.third_party.at.asitplus.oidc.dcql.toDefaultSubmission
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.ktor.openid.OpenId4VpWallet
import data.storage.PersistentCookieStorage
import io.github.aakira.napier.Napier
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType.Application.FormUrlEncoded
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import org.jetbrains.compose.resources.getString

abstract class AuthenticationViewModel(
    val spName: String?,
    val spLocation: String,
    val spImage: ImageBitmap?,
    val navigateUp: () -> Unit,
    val onAuthenticationSuccess: (redirectUrl: String?) -> Unit,
    val navigateToHomeScreen: () -> Unit,
    val walletMain: WalletMain,
    val onClickLogo: () -> Unit,
    val onClickSettings: () -> Unit
) {
    abstract val presentationRequest: CredentialPresentationRequest

    var viewState by mutableStateOf(AuthenticationViewState.Consent)
    abstract val transactionData: TransactionDataBase64Url?

    lateinit var matchingCredentials: CredentialMatchingResult<SubjectCredentialStore.StoreEntry>
    lateinit var defaultCredentialSelection: Map<String, SubjectCredentialStore.StoreEntry>

    abstract suspend fun findMatchingCredentials(): KmmResult<CredentialMatchingResult<SubjectCredentialStore.StoreEntry>>

    suspend fun onConsent() {
        matchingCredentials = findMatchingCredentials().getOrElse {
            viewState = AuthenticationViewState.NoMatchingCredential
            return
        }

        when (val matching = matchingCredentials) {
            is DCQLMatchingResult -> {
                matching.dcqlQueryResult.toDefaultSubmission()
                // TODO: create default selection?
                // matching fails if query is not satisfiable, so we know that selection is the next step
                viewState = AuthenticationViewState.Selection
            }

            is PresentationExchangeMatchingResult -> {
                if (matching.matchingInputDescriptorCredentials.values.find { it.size != 1 } == null) {
                    defaultCredentialSelection = matching.matchingInputDescriptorCredentials.entries.associate {
                        val requestId = it.key
                        val credential = it.value.keys.first()
                        requestId to credential
                    }.toMap()
                    viewState = AuthenticationViewState.Selection
                } else if (matching.matchingInputDescriptorCredentials.values.find { it.isEmpty() } == null) {
                    viewState = AuthenticationViewState.Selection
                } else {
                    //viewState = AuthenticationViewState.NoMatchingCredential
                    sendError()
                }
            }
        }
    }

    @OptIn(InternalAPI::class)
    suspend fun sendError() {
        val cookieStorage = AcceptAllCookiesStorage()
        val client = walletMain.httpService.buildHttpClient(cookieStorage)
        when (this){
            is DefaultAuthenticationViewModel -> {
                val state = this.authenticationRequest.parameters.state!!
                val nonce = this.authenticationRequest.parameters.nonce!!
                this.authenticationRequest.parameters.responseUrl?.let {
                    val response = client.post(it) {
                        contentType(FormUrlEncoded)
                        body = FormDataContent(Parameters.build {
                            append("error", "invalid_request")
                            append("error_description", "unsupported client_id_prefix")
                            append("state", state)
                        })
                    }
                    Napier.e(response.toString())
                }

            }
            else -> {

            }
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

                    is PresentationExchangeCredentialSubmissions -> CredentialPresentation.PresentationExchangePresentation(
                        presentationRequest = presentationRequest as CredentialPresentationRequest.PresentationExchangeRequest,
                        inputDescriptorSubmissions = credentialPresentationSubmissions.inputDescriptorSubmissions
                    )

                    null -> when(val it = presentationRequest) {
                        is CredentialPresentationRequest.DCQLRequest -> CredentialPresentation.DCQLPresentation(
                            presentationRequest = it,
                            credentialQuerySubmissions = null
                        )

                        is CredentialPresentationRequest.PresentationExchangeRequest -> CredentialPresentation.PresentationExchangePresentation(
                            presentationRequest = it,
                            inputDescriptorSubmissions = null,
                        )
                    }
                }
            )
        }
    }


    abstract suspend fun finalizationMethod(credentialPresentation: CredentialPresentation): OpenId4VpWallet.AuthenticationSuccess

    private suspend fun finalizeAuthorization(credentialPresentation: CredentialPresentation) {
        catchingUnwrapped {
            walletMain.keyMaterial.promptText =
                getString(Res.string.biometric_authentication_prompt_for_data_transmission_consent_title)
            walletMain.keyMaterial.promptSubtitle =
                getString(Res.string.biometric_authentication_prompt_for_data_transmission_consent_subtitle)
            finalizationMethod(credentialPresentation)
        }.onSuccess {
            navigateUp()
            onAuthenticationSuccess(it.redirectUri)
        }.onFailure {
            walletMain.errorService.emit(it)
        }
    }
}

enum class AuthenticationViewState {
    Consent,
    NoMatchingCredential,
    Selection
}

@Serializable
data class err (val error: String, val error_description: String, val state: String)