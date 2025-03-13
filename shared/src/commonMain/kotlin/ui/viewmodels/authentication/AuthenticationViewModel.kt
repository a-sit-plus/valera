package ui.viewmodels.authentication

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.catchingUnwrapped
import at.asitplus.dif.ConstraintField
import at.asitplus.dif.InputDescriptor
import at.asitplus.jsonpath.core.NodeList
import at.asitplus.rqes.collection_entries.TransactionData
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.biometric_authentication_prompt_for_data_transmission_consent_subtitle
import at.asitplus.valera.resources.biometric_authentication_prompt_for_data_transmission_consent_title
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.agent.CredentialSubmission
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

abstract class AuthenticationViewModel(
    val spName: String?,
    val spLocation: String,
    val spImage: ImageBitmap?,
    val navigateUp: () -> Unit,
    val navigateToAuthenticationSuccessPage: () -> Unit,
    val navigateToHomeScreen: () -> Unit,
    val walletMain: WalletMain,
    val onClickLogo: () -> Unit
) {
    abstract val descriptors: Collection<InputDescriptor>
    var viewState by mutableStateOf(AuthenticationViewState.Consent)
    abstract val transactionData: TransactionData?

    lateinit var matchingCredentials: Map<String, Map<SubjectCredentialStore.StoreEntry, Map<ConstraintField, NodeList /* = List<NodeListEntry> */>>>
    lateinit var selectedCredentials: Map<String, SubjectCredentialStore.StoreEntry>
    var requestMap: Map<String, Map<SubjectCredentialStore.StoreEntry, Map<ConstraintField, NodeList>>> =
        mutableMapOf()

    abstract fun findMatchingCredentials(): Map<String, Map<SubjectCredentialStore.StoreEntry, Map<ConstraintField, NodeList /* = List<NodeListEntry> */>>>

    fun onConsent() {
        matchingCredentials = findMatchingCredentials()

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

    abstract suspend fun finalizationMethod(submission: Map<String, CredentialSubmission>)


    private suspend fun finalizeAuthorization(submission: Map<String, CredentialSubmission>) {
        catchingUnwrapped{
            walletMain.cryptoService.promptText =
                getString(Res.string.biometric_authentication_prompt_for_data_transmission_consent_title)
            walletMain.cryptoService.promptSubtitle =
                getString(Res.string.biometric_authentication_prompt_for_data_transmission_consent_subtitle)
            finalizationMethod(submission)
        }.onSuccess {
            navigateUp()
            navigateToAuthenticationSuccessPage()
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