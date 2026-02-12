package ui.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.unexpected_screen_text
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.openid.CredentialMatchingResult
import at.asitplus.wallet.lib.openid.DCQLMatchingResult
import at.asitplus.wallet.lib.openid.PresentationExchangeMatchingResult
import org.jetbrains.compose.resources.stringResource
import ui.composables.DCQLCredentialQuerySubmissionSelectionOption
import ui.composables.DelayedComposable
import ui.viewmodels.authentication.CredentialPresentationSubmissions
import ui.viewmodels.authentication.DCQLCredentialSubmissions
import ui.views.LoadingView
import kotlin.time.Duration.Companion.seconds

@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@Composable
fun PresentationBuilderGraphView(
    authenticateAtRelyingParty: Boolean,
    serviceProviderLocalizedName: String?,
    serviceProviderLocalizedLocation: String,
    matchingResult: UiState<CredentialMatchingResult<SubjectCredentialStore.StoreEntry>>,
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    onError: (Throwable) -> Unit,
    onNavigateToPresentationStart: () -> Unit,
    onSubmit: (CredentialPresentationSubmissions<SubjectCredentialStore.StoreEntry>) -> Unit,
) {
    when (matchingResult) {
        is UiStateError -> CommonPresentationPageScaffold(
            onClickLogo = onClickLogo,
            onClickSettings = onClickSettings,
            onNavigateUp = onNavigateToPresentationStart,
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DelayedComposable(1.seconds) {
                    // parent should've redirected to error page in this case
                    Text(stringResource(Res.string.unexpected_screen_text))
                }
            }
        }

        UiStateLoading -> CommonPresentationPageScaffold(
            onClickLogo = onClickLogo,
            onClickSettings = onClickSettings,
            onNavigateUp = onNavigateToPresentationStart,
        ) {
            LoadingView()
        }

        is UiStateSuccess -> when (matchingResult.value) {
            is DCQLMatchingResult -> DCQLPresentationBuilderGraphView(
                authenticateAtRelyingParty = authenticateAtRelyingParty,
                serviceProviderLocalizedLocation = serviceProviderLocalizedLocation,
                serviceProviderLocalizedName = serviceProviderLocalizedName,
                onClickLogo = onClickLogo,
                onClickSettings = onClickSettings,
                dcqlQuery = matchingResult.value.presentationRequest.dcqlQuery,
                satisfiableCredentialQueries = matchingResult.value.dcqlQueryResult.credentialQueryMatches.filter {
                    it.value.isNotEmpty()
                }.keys,
                onError = onError,
                onNavigateUp = onNavigateToPresentationStart,
                selectableCredentialSubmissionCards = matchingResult.value.dcqlQueryResult.credentialQueryMatches.mapValues {
                    it.value.map { option ->
                        SelectableCredentialSubmissionCard { isSelected, allowMultiSelection, onToggleSelection ->
                            DCQLCredentialQuerySubmissionSelectionOption(
                                allowMultiSelection = allowMultiSelection,
                                isSelected = isSelected,
                                onToggleSelection = onToggleSelection,
                                option = option,
                            )
                        }
                    }
                },
                onSubmit = {
                    val submissions = it.mapValues { (queryId, submissionIndices) ->
                        val matches = matchingResult.value.dcqlQueryResult.credentialQueryMatches[queryId]
                            ?: return@DCQLPresentationBuilderGraphView onError(IllegalStateException("Failed to find submission options for unknown credential query identifier $queryId"))
                        submissionIndices.map {
                            matches.getOrNull(it.toInt()) ?: return@DCQLPresentationBuilderGraphView onError(
                                IllegalStateException("Failed to find submission option index $it for credential query identifier $queryId")
                            )
                        }
                    }
                    // TODO: requires newer vck version for list of submissions per id
                    onSubmit(
                        DCQLCredentialSubmissions(submissions)
                    )
                }
            )

            is PresentationExchangeMatchingResult -> PresentationExchangePresentationBuilderGraphView(
                authenticateAtRelyingParty = authenticateAtRelyingParty,
                serviceProviderLocalizedLocation = serviceProviderLocalizedLocation,
                serviceProviderLocalizedName = serviceProviderLocalizedName,
                onClickLogo = onClickLogo,
                onClickSettings = onClickSettings,
                matchingResult = matchingResult.value,
                onError = onError,
                onNavigateUp = onNavigateToPresentationStart,
                onSubmit = onSubmit,
            )
        }
    }
}

