package ui.viewmodels.Authentication

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.dif.ConstraintField
import at.asitplus.jsonpath.core.NodeList
import at.asitplus.misc.getRequestOptionParameters
import at.asitplus.openid.AuthenticationRequestParameters
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.biometric_authentication_prompt_for_data_transmission_consent_subtitle
import at.asitplus.valera.resources.biometric_authentication_prompt_for_data_transmission_consent_title
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.agent.CredentialSubmission
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.oidc.helpers.AuthorizationResponsePreparationState
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

class AuthenticationViewModel(
    val spName: String?,
    val spLocation: String,
    val spImage: ImageBitmap?,
    val authenticationRequest: RequestParametersFrom<AuthenticationRequestParameters>,
    val navigateUp: () -> Unit,
    val navigateToAuthenticationSuccessPage: () -> Unit,
    val navigateToHomeScreen: () -> Unit,
    val walletMain: WalletMain,
) {
    var viewState by mutableStateOf(AuthenticationViewState.Consent)
    var descriptors =
        authenticationRequest.parameters.presentationDefinition?.inputDescriptors ?: listOf()
    var parametersMap = descriptors.mapNotNull {
        val parameter = it.getRequestOptionParameters() ?: return@mapNotNull null
        Pair(it.id, parameter)
    }.toMap()

    lateinit var preparationState: AuthorizationResponsePreparationState
    lateinit var matchingCredentials: Map<String, Map<SubjectCredentialStore.StoreEntry, Map<ConstraintField, NodeList /* = List<NodeListEntry> */>>>
    lateinit var selectedCredentials: Map<String, SubjectCredentialStore.StoreEntry>
    var requestMap: Map<String, Map<SubjectCredentialStore.StoreEntry, Map<ConstraintField, NodeList>>> =
        mutableMapOf()

    fun onConsent() {
        preparationState =
            runBlocking { walletMain.presentationService.getPreparationState(request = authenticationRequest) }
        matchingCredentials =
            runBlocking { walletMain.presentationService.getMatchingCredentials(preparationState = preparationState) }

        requestMap = descriptors.mapNotNull {
            val credential = matchingCredentials[it.id] ?: return@mapNotNull null
            Pair(it.id, credential)
        }.toMap()
        
        if (matchingCredentials.values.find { it.size != 1 } == null) {
            selectedCredentials = matchingCredentials.entries.associate {
                val requestId = it.key
                val credential = it.value.keys.first()
                requestId to credential
            }.toMap()
            viewState = AuthenticationViewState.Selection
        } else if (matchingCredentials.values.find { it.isEmpty() } == null) {
            viewState = AuthenticationViewState.Selection
        } else {
            viewState = AuthenticationViewState.NoMatchingCredential
        }
    }

    fun confirmSelection(submissions: Map<String, CredentialSubmission>) {
        walletMain.scope.launch {
            finalizeAuthorization(submissions)
        }
    }

    private suspend fun finalizeAuthorization(submission: Map<String, CredentialSubmission>) {
        try {
            walletMain.cryptoService.promptText =
                getString(Res.string.biometric_authentication_prompt_for_data_transmission_consent_title)
            walletMain.cryptoService.promptSubtitle =
                getString(Res.string.biometric_authentication_prompt_for_data_transmission_consent_subtitle)
            walletMain.presentationService.finalizeAuthorizationResponse(
                request = authenticationRequest,
                preparationState = preparationState,
                inputDescriptorSubmission = submission
            )
            navigateUp()
            navigateToAuthenticationSuccessPage()
        } catch (e: Throwable) {
            walletMain.errorService.emit(e)
        }
    }
}

enum class AuthenticationViewState {
    Consent,
    NoMatchingCredential,
    Selection
}