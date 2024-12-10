package ui.viewmodels.Authentication

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.dif.ConstraintField
import at.asitplus.jsonpath.core.NodeList
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.misc.getRequestOptionParameters
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.agent.CredentialSubmission
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.oidc.AuthenticationRequestParametersFrom
import at.asitplus.wallet.lib.oidc.helpers.AuthorizationResponsePreparationState
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.biometric_authentication_prompt_for_data_transmission_consent_subtitle
import compose_wallet_app.shared.generated.resources.biometric_authentication_prompt_for_data_transmission_consent_title
import data.RequestOptionParameters
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

class AuthenticationViewModel(
    val spName: String?,
    val spLocation: String,
    val spImage: ImageBitmap?,
    val authenticationRequest: AuthenticationRequestParametersFrom,
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
    lateinit var selectedCredentials: Map<String, SubjectCredentialStore.StoreEntry>
    var requestMap: Map<String, Pair<RequestOptionParameters, Map<SubjectCredentialStore.StoreEntry, Map<ConstraintField, NodeList>>>> =
        mutableMapOf()

    fun onConsent() {
        preparationState =
            runBlocking { walletMain.presentationService.getPreparationState(request = authenticationRequest) }
        val matchingCredentials =
            runBlocking { walletMain.presentationService.getMatchingCredentials(preparationState = preparationState) }

        requestMap = descriptors.mapNotNull {
            val parameter = parametersMap[it.id] ?: return@mapNotNull null
            val credential = matchingCredentials[it.id] ?: return@mapNotNull null
            Pair(it.id, Pair(parameter, credential))
        }.toMap()

        if (matchingCredentials.values.find { it.size != 1 } == null) {
            selectedCredentials = matchingCredentials.entries.associate { it.key to it.value.keys.first()}.toMap()
            viewState = AuthenticationViewState.AttributesSelection
        } else if (matchingCredentials.values.find { it.isEmpty() } == null) {
            viewState = AuthenticationViewState.CredentialSelection
        } else {
            viewState = AuthenticationViewState.NoMatchingCredential
        }
    }

    fun selectCredential(credentials: Map<String, SubjectCredentialStore.StoreEntry>) {
        selectedCredentials = credentials
        viewState = AuthenticationViewState.AttributesSelection
    }

    fun selectAttributes(selectedAttributes: Map<String, Set<NormalizedJsonPath>>) {
        walletMain.scope.launch {
            val submissions = descriptors.mapNotNull {
                val credential = selectedCredentials[it.id] ?: return@mapNotNull null
                val disclosedAttributes = selectedAttributes[it.id] ?: return@mapNotNull null
                Pair(it.id, CredentialSubmission(credential, disclosedAttributes))
            }.toMap()

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
    CredentialSelection,
    AttributesSelection,
    NoMatchingCredential
}