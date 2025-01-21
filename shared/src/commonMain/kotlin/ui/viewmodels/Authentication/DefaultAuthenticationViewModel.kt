package ui.viewmodels.Authentication

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.dif.ConstraintField
import at.asitplus.jsonpath.core.NodeList
import at.asitplus.misc.getRequestOptionParameters
import at.asitplus.openid.AuthenticationRequestParameters
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.rqes.collection_entries.TransactionData
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.agent.CredentialSubmission
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.openid.AuthorizationResponsePreparationState
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.oidc.helpers.AuthorizationResponsePreparationState
import kotlinx.coroutines.runBlocking


class DefaultAuthenticationViewModel(
    spName: String?,
    spLocation: String,
    spImage: ImageBitmap?,
    val  authenticationRequest: RequestParametersFrom<AuthenticationRequestParameters>,
    navigateUp: () -> Unit,
    navigateToAuthenticationSuccessPage: () -> Unit,
    navigateToHomeScreen: () -> Unit,
    walletMain: WalletMain
) : AuthenticationViewModel(
    spName,
    spLocation,
    spImage,
    navigateUp,
    navigateToAuthenticationSuccessPage,
    navigateToHomeScreen,
    walletMain
) {
    override val descriptors =
        authenticationRequest.parameters.presentationDefinition?.inputDescriptors ?: listOf()
    override val parametersMap = descriptors.mapNotNull {
        val parameter = it.getRequestOptionParameters().getOrElse { return@mapNotNull null }
        Pair(it.id, parameter)
    }.toMap()

    override val transactionData = try {
        vckJsonSerializer.decodeFromString<TransactionData>(authenticationRequest.parameters.transactionData?.first()!!)
    } catch (e: Throwable)
    { null }

    private lateinit var preparationState: AuthorizationResponsePreparationState

    override fun findMatchingCredentials(): Map<String, Map<SubjectCredentialStore.StoreEntry, Map<ConstraintField, NodeList>>> {
        preparationState =
            runBlocking { walletMain.presentationService.getPreparationState(request = authenticationRequest) }
        return runBlocking { walletMain.presentationService.getMatchingCredentials(preparationState = preparationState) }
    }

    override suspend fun finalizationMethod(submission: Map<String, CredentialSubmission>) =
        walletMain.presentationService.finalizeAuthorizationResponse(
            request = authenticationRequest,
            preparationState = preparationState,
            inputDescriptorSubmission = submission
        )

}