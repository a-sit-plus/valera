package ui.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import at.asitplus.KmmResult
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.catching
import at.asitplus.openid.dcql.DCQLCredentialQueryIdentifier
import at.asitplus.openid.dcql.DCQLCredentialQueryMatchingResult
import at.asitplus.openid.dcql.DCQLCredentialSubmissionOption
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.unexpected_screen_text
import at.asitplus.wallet.app.common.LoadingMessageKey
import at.asitplus.wallet.app.common.TrustListService
import at.asitplus.wallet.lib.agent.DCQLMatchingResult
import at.asitplus.wallet.lib.agent.IsoDeviceRetrievalMatchingResult
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource
import ui.composables.DCQLCredentialQuerySubmissionSelectionOption
import ui.composables.DelayedComposable
import ui.models.CredentialFreshnessValidationStateUiModel
import ui.viewmodels.authentication.AuthenticationNoCredentialViewModel
import ui.viewmodels.authentication.CredentialPresentationSubmissions
import ui.viewmodels.authentication.DCQLCredentialSubmissions
import ui.views.LoadingView
import ui.views.authentication.AuthenticationNoCredentialView
import kotlin.time.Duration.Companion.seconds

private typealias FixedDcApiSubmissions = Map<
    DCQLCredentialQueryIdentifier,
    List<DCQLCredentialSubmissionOption<SubjectCredentialStore.StoreEntry>>,
>

@ExperimentalMaterial3Api
@Composable
fun PresentationBuilderGraphView(
    title: String,
    authenticateAtRelyingParty: Boolean,
    serviceProviderLocalizedName: String?,
    serviceProviderLocalizedLocation: String,
    selectionProvider: UiState<CredentialSelectionProvider<SubjectCredentialStore.StoreEntry>>,
    onClickLogo: () -> Unit,
    onError: (Throwable) -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateToPresentationStart: () -> Unit,
    onSubmit: (CredentialPresentationSubmissions<SubjectCredentialStore.StoreEntry>) -> Unit,
    trustListService: TrustListService,
    request: RequestParametersFrom<*>,
    fixedCredentialSelection: Boolean = false,
) {
    when (selectionProvider) {
        is UiStateError -> CommonPresentationPageScaffold(
            title = title,
            onClickLogo = onClickLogo,
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
            title = title,
            onClickLogo = onClickLogo,
            onNavigateUp = onNavigateToPresentationStart,
        ) {
            LoadingView(ui.views.loadingMessageString(LoadingMessageKey.CheckingRequestedCredentials))
        }

        is UiStateSuccess -> {
            when (val queryMatchingResult = selectionProvider.value.queryMatchingResult) {
                is DCQLMatchingResult -> {
                    val selectableCredentialSubmissionCards = queryMatchingResult.toSelectableCredentialSubmissionCards(
                        credentialFreshnessProviders = selectionProvider.value.credentialFreshnessProviders,
                        trustListService = trustListService,
                    )
                    if (fixedCredentialSelection) {
                        val fixedSubmissionsResult = queryMatchingResult.toFixedDcApiSubmissions()
                        fixedSubmissionsResult.exceptionOrNull()?.let { throwable ->
                            LaunchedEffect(throwable.message) {
                                onError(throwable)
                            }
                            return
                        }
                        val fixedSubmissions = fixedSubmissionsResult.getOrThrow()
                        val fixedSubmissionCards = try {
                            fixedSubmissions.toSelectableCredentialSubmissionCards(
                                selectionProvider = selectionProvider.value,
                                trustListService = trustListService,
                            )
                        } catch (throwable: Throwable) {
                            LaunchedEffect(throwable.message) {
                                onError(throwable)
                            }
                            return
                        }
                        DCQLPresentationFinalizationPageContent(
                            authenticateAtRelyingParty = authenticateAtRelyingParty,
                            serviceProviderLocalizedLocation = serviceProviderLocalizedLocation,
                            serviceProviderLocalizedName = serviceProviderLocalizedName,
                            dcqlQuery = queryMatchingResult.presentationRequest.dcqlQuery,
                            selections = fixedSubmissionCards,
                            onError = onError,
                            onAbort = onNavigateToPresentationStart,
                            onSubmit = {
                                onSubmit(DCQLCredentialSubmissions(fixedSubmissions))
                            },
                            trustListService = trustListService,
                            request = request
                        )
                    } else {
                        DCQLPresentationBuilderGraphView(
                            title = title,
                            authenticateAtRelyingParty = authenticateAtRelyingParty,
                            serviceProviderLocalizedLocation = serviceProviderLocalizedLocation,
                            serviceProviderLocalizedName = serviceProviderLocalizedName,
                            onClickLogo = onClickLogo,
                            dcqlQuery = queryMatchingResult.presentationRequest.dcqlQuery,
                            satisfiableCredentialQueries = queryMatchingResult.matchingResult.credentialQueryMatches.filter {
                                it.value.isNotEmpty()
                            }.keys,
                            onError = onError,
                            onNavigateUp = onNavigateToPresentationStart,
                            selectableCredentialSubmissionCards = selectableCredentialSubmissionCards,
                            onSubmit = {
                                val submissions = it.mapValues { (queryId, submissionIndices) ->
                                    val matches =
                                        selectionProvider.value.queryMatchingResult.matchingResult.dcqlQueryMatchingResult.credentialMatchingResults[queryId]
                                            ?: return@DCQLPresentationBuilderGraphView onError(IllegalStateException("Failed to find submission options for unknown credential query identifier $queryId"))
                                    submissionIndices.map {
                                        val credentialMatchingResult = matches.getOrNull(it.toInt())?.getOrNull()
                                            ?: return@DCQLPresentationBuilderGraphView onError(
                                                IllegalStateException("Failed to find submission option index $it for credential query identifier $queryId")
                                            )
                                        val credential =
                                            selectionProvider.value.queryMatchingResult.matchingResult.credentials.getOrNull(
                                                it.toInt()
                                            ) ?: return@DCQLPresentationBuilderGraphView onError(
                                                IllegalStateException("Failed to find credential at index $it")
                                            )
                                        DCQLCredentialSubmissionOption(
                                            credential = credential,
                                            matchingResult = credentialMatchingResult,
                                        )
                                    }
                                }

                                onSubmit(DCQLCredentialSubmissions(submissions))
                            },
                            trustListService = trustListService,
                            request = request
                        )
                    }
                }

                is at.asitplus.wallet.lib.agent.PresentationExchangeMatchingResult -> if (
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
                    if (fixedCredentialSelection) {
                        val fixedSubmissions = queryMatchingResult.matchingResult.toDefaultSubmission()
                        if (fixedSubmissions.isEmpty()) {
                            LaunchedEffect(Unit) {
                                onError(
                                    IllegalStateException(
                                        "No credential matching the fixed DC API selection was found"
                                    )
                                )
                            }
                            return
                        }
                        PresentationExchangeFinalizationPageContent(
                            matchingResult = queryMatchingResult,
                            credentialFreshnessProviders = selectionProvider.value.credentialFreshnessProviders,
                            inputDescriptorSubmissions = fixedSubmissions,
                            trustListService = trustListService,
                            authenticateAtRelyingParty = authenticateAtRelyingParty,
                            serviceProviderLocalizedLocation = serviceProviderLocalizedLocation,
                            serviceProviderLocalizedName = serviceProviderLocalizedName,
                            onError = onError,
                            onAbort = onNavigateToPresentationStart,
                            onSubmit = {
                                onSubmit(it)
                            },
                        )
                    } else {
                        PresentationExchangePresentationBuilderGraphView(
                            authenticateAtRelyingParty = authenticateAtRelyingParty,
                            serviceProviderLocalizedLocation = serviceProviderLocalizedLocation,
                            serviceProviderLocalizedName = serviceProviderLocalizedName,
                            onClickLogo = onClickLogo,
                            matchingResult = selectionProvider.value.queryMatchingResult,
                            onError = onError,
                            onNavigateUp = onNavigateToPresentationStart,
                            onSubmit = onSubmit,
                            trustListService = trustListService
                        )
                    }
                }

                is IsoDeviceRetrievalMatchingResult<*> -> TODO()
            }
        }
    }
}

private fun DCQLMatchingResult<SubjectCredentialStore.StoreEntry>.toFixedDcApiSubmissions():
    KmmResult<FixedDcApiSubmissions> =
    catching {
        val dcqlQuery = presentationRequest.dcqlQuery
        val queriesAllowingMultiple = dcqlQuery.credentials
            .filter { it.multiple }
            .map { it.id }
            .toSet()
        val submissions = matchingResult.credentialQueryMatches
            .mapValues { (queryId, matches) ->
                if (queryId in queriesAllowingMultiple) matches else matches.take(1)
            }
            .filterValues { it.isNotEmpty() }

        require(submissions.isNotEmpty()) {
            "No credential matching the fixed DC API selection was found"
        }
        dcqlQuery.checkCredentialSetQueryRequirements(submissions.keys).getOrThrow()
        submissions
    }

private fun DCQLMatchingResult<SubjectCredentialStore.StoreEntry>.toSelectableCredentialSubmissionCards(
    credentialFreshnessProviders: List<StateFlow<CredentialFreshnessValidationStateUiModel>>,
    trustListService: TrustListService,
): Map<DCQLCredentialQueryIdentifier, List<SelectableCredentialSubmissionCard>> {
    val credentials = matchingResult.credentials.zip(credentialFreshnessProviders)
    return matchingResult.dcqlQueryMatchingResult.credentialMatchingResults.mapValues {
        it.value.zip(credentials) { matchingResult, (credential, freshnessState) ->
            dcqlCredentialSubmissionCard(
                credential = credential,
                matchingResult = matchingResult,
                freshnessState = freshnessState,
                trustListService = trustListService,
            )
        }
    }
}

private fun Map<DCQLCredentialQueryIdentifier, List<DCQLCredentialSubmissionOption<SubjectCredentialStore.StoreEntry>>>.toSelectableCredentialSubmissionCards(
    selectionProvider: CredentialSelectionProvider<SubjectCredentialStore.StoreEntry>,
    trustListService: TrustListService,
): Map<DCQLCredentialQueryIdentifier, List<SelectableCredentialSubmissionCard>> {
    val freshnessProvidersByCredential =
        selectionProvider.queryMatchingResult.matchingResult.credentials
            .zip(selectionProvider.credentialFreshnessProviders)
            .toMap()

    return mapValues { (queryId, submissions) ->
        submissions.map { submission ->
            val freshnessState = freshnessProvidersByCredential[submission.credential]
                ?: throw IllegalStateException(
                    "Failed to find freshness provider for credential query identifier $queryId"
                )
            dcqlCredentialSubmissionCard(
                credential = submission.credential,
                matchingResult = KmmResult.success(submission.matchingResult),
                freshnessState = freshnessState,
                trustListService = trustListService,
            )
        }
    }
}

private fun dcqlCredentialSubmissionCard(
    credential: SubjectCredentialStore.StoreEntry,
    matchingResult: KmmResult<DCQLCredentialQueryMatchingResult>,
    freshnessState: StateFlow<CredentialFreshnessValidationStateUiModel>,
    trustListService: TrustListService,
) = object : SelectableCredentialSubmissionCard {
    @Composable
    override fun invoke(
        isSelected: Boolean,
        allowMultiSelection: Boolean,
        onToggleSelection: (() -> Unit)?,
        onLoadingChanged: (Boolean) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        DCQLCredentialQuerySubmissionSelectionOption(
            allowMultiSelection = allowMultiSelection,
            isSelected = isSelected,
            onToggleSelection = onToggleSelection,
            credential = credential,
            matchingResult = matchingResult,
            freshnessState = freshnessState,
            trustListService = trustListService,
            onLoadingChanged = onLoadingChanged,
            onError = onError,
        )
    }

    override val credentialFreshnessSummary = freshnessState
    override val matchingException = matchingResult.exceptionOrNull()
}
