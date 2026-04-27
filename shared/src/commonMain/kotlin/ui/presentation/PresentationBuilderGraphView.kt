package ui.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import at.asitplus.openid.dcql.DCQLCredentialSubmissionOption
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.unexpected_screen_text
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.openid.DCQLMatchingResult
import at.asitplus.wallet.lib.openid.PresentationExchangeMatchingResult
import org.jetbrains.compose.resources.stringResource
import ui.composables.DCQLCredentialQuerySubmissionSelectionOption
import ui.composables.DelayedComposable
import ui.viewmodels.authentication.AuthenticationNoCredentialViewModel
import ui.viewmodels.authentication.CredentialPresentationSubmissions
import ui.viewmodels.authentication.DCQLCredentialSubmissions
import ui.views.authentication.AuthenticationNoCredentialView
import ui.views.LoadingView
import kotlin.time.Duration.Companion.seconds

@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@Composable
fun PresentationBuilderGraphView(
    authenticateAtRelyingParty: Boolean,
    serviceProviderLocalizedName: String?,
    serviceProviderLocalizedLocation: String,
    selectionProvider: UiState<CredentialSelectionProvider<SubjectCredentialStore.StoreEntry>>,
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    onError: (Throwable) -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateToPresentationStart: () -> Unit,
    onSubmit: (CredentialPresentationSubmissions<SubjectCredentialStore.StoreEntry>) -> Unit,
) {
    when (selectionProvider) {
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

        is UiStateSuccess -> {
            when (val queryMatchingResult = selectionProvider.value.queryMatchingResult) {
                is DCQLMatchingResult -> {
                    DCQLPresentationBuilderGraphView(
                        authenticateAtRelyingParty = authenticateAtRelyingParty,
                        serviceProviderLocalizedLocation = serviceProviderLocalizedLocation,
                        serviceProviderLocalizedName = serviceProviderLocalizedName,
                        onClickLogo = onClickLogo,
                        onClickSettings = onClickSettings,
                        dcqlQuery = queryMatchingResult.presentationRequest.dcqlQuery,
                        satisfiableCredentialQueries = queryMatchingResult.matchingResult.credentialQueryMatches.filter {
                            it.value.isNotEmpty()
                        }.keys,
                        onError = onError,
                        onNavigateUp = onNavigateToPresentationStart,
                        selectableCredentialSubmissionCards = queryMatchingResult.let {
                            val credentials = it.matchingResult.credentials.zip(selectionProvider.value.credentialFreshnessProviders)
                            it.matchingResult.dcqlQueryMatchingResult.credentialMatchingResults.mapValues {
                                it.value.zip(credentials) { matchingResult, (credential, freshnessState) ->
                                    object : SelectableCredentialSubmissionCard {
                                        @Composable
                                        override fun invoke(
                                            isSelected: Boolean,
                                            allowMultiSelection: Boolean,
                                            onToggleSelection: (() -> Unit)?
                                        ) {
                                            DCQLCredentialQuerySubmissionSelectionOption(
                                                allowMultiSelection = allowMultiSelection,
                                                isSelected = isSelected,
                                                onToggleSelection = onToggleSelection,
                                                credential = credential,
                                                matchingResult = matchingResult,
                                                freshnessState = freshnessState,
                                            )
                                        }

                                        override val credentialFreshnessSummary = freshnessState
                                        override val matchingException = matchingResult.exceptionOrNull()
                                    }
                                }
                            }
                        },
                        onSubmit = {
                            val submissions = it.mapValues { (queryId, submissionIndices) ->
                                val matches = selectionProvider.value.queryMatchingResult.matchingResult.dcqlQueryMatchingResult.credentialMatchingResults[queryId]
                                    ?: return@DCQLPresentationBuilderGraphView onError(IllegalStateException("Failed to find submission options for unknown credential query identifier $queryId"))
                                submissionIndices.map {
                                    val credentialMatchingResult = matches.getOrNull(it.toInt())?.getOrNull() ?: return@DCQLPresentationBuilderGraphView onError(
                                        IllegalStateException("Failed to find submission option index $it for credential query identifier $queryId")
                                    )
                                    val credential = selectionProvider.value.queryMatchingResult.matchingResult.credentials.getOrNull(it.toInt()) ?: return@DCQLPresentationBuilderGraphView onError(
                                        IllegalStateException("Failed to find credential at index $it")
                                    )
                                    DCQLCredentialSubmissionOption(
                                        credential = credential,
                                        matchingResult = credentialMatchingResult,
                                    )
                                }
                            }

                            onSubmit(DCQLCredentialSubmissions(submissions))
                        }
                    )
                }

                is PresentationExchangeMatchingResult -> if (
                    hasMissingPresentationExchangeInputDescriptorMatches(
                        queryMatchingResult.matchingResult.inputDescriptorMatches
                    )
                ) {
                    AuthenticationNoCredentialView(
                        AuthenticationNoCredentialViewModel(
                            navigateToHomeScreen = onNavigateUp,
                        )
                    )
                } else {
                    PresentationExchangePresentationBuilderGraphView(
                        authenticateAtRelyingParty = authenticateAtRelyingParty,
                        serviceProviderLocalizedLocation = serviceProviderLocalizedLocation,
                        serviceProviderLocalizedName = serviceProviderLocalizedName,
                        onClickLogo = onClickLogo,
                        onClickSettings = onClickSettings,
                        matchingResult = selectionProvider.value.queryMatchingResult,
                        onError = onError,
                        onNavigateUp = onNavigateToPresentationStart,
                        onSubmit = onSubmit,
                    )
                }
            }
        }
    }
}
