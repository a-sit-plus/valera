package ui.presentation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import at.asitplus.openid.dcql.DCQLCredentialQueryIdentifier
import at.asitplus.openid.dcql.DCQLQuery
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.openid.PresentationExchangeMatchingResult
import org.koin.compose.viewmodel.koinViewModel
import ui.viewmodels.authentication.AuthenticationSelectionPresentationExchangeViewModel
import ui.viewmodels.authentication.CredentialPresentationSubmissions
import ui.views.authentication.AuthenticationSelectionPresentationExchangeView

@ExperimentalMaterial3Api
@Composable
fun PresentationExchangePresentationBuilderGraphView(
    matchingResult: PresentationExchangeMatchingResult<SubjectCredentialStore.StoreEntry>,
    authenticateAtRelyingParty: Boolean,
    serviceProviderLocalizedName: String?,
    serviceProviderLocalizedLocation: String,
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    /**
     * This delegates to library users how to display the credential cards.
     * A library user may also include credentials that did not match and simply not invoke the selection function.
     */
    onError: (Throwable) -> Unit,
    onNavigateUp: () -> Unit,
    onSubmit: (CredentialPresentationSubmissions<SubjectCredentialStore.StoreEntry>) -> Unit,
) {
    AuthenticationSelectionPresentationExchangeView(
        onClickLogo = onClickLogo,
        onClickSettings = onClickSettings,
        vm = AuthenticationSelectionPresentationExchangeViewModel(
            confirmSelections = { selections -> onSubmit(selections) },
            navigateUp = onNavigateUp,
            credentialMatchingResult = matchingResult
        ),
        onError = onError,
    )
}