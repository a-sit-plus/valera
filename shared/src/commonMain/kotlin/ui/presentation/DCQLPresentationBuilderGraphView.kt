package ui.presentation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import at.asitplus.openid.dcql.DCQLCredentialQueryIdentifier
import at.asitplus.openid.dcql.DCQLQuery

@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@Composable
fun DCQLPresentationBuilderGraphView(
    authenticateAtRelyingParty: Boolean,
    serviceProviderLocalizedName: String?,
    serviceProviderLocalizedLocation: String,
    dcqlQuery: DCQLQuery,
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    /**
     * This delegates to library users how to display the credential cards.
     * A library user may also include credentials that did not match and simply not invoke the selection function.
     */
    selectableCredentialSubmissionCards: Map<DCQLCredentialQueryIdentifier, List<Pair<Boolean, SelectableCredentialSubmissionCard>>>,
    satisfiableCredentialQueries: Collection<DCQLCredentialQueryIdentifier>,
    onError: (Throwable) -> Unit,
    onNavigateUp: () -> Unit,
    onSubmit: (Map<DCQLCredentialQueryIdentifier, Set<UInt>>) -> Unit,
) {
    var counter by rememberSaveable {
        mutableStateOf(0)
    }
    counter += 1
    val viewModel = rememberSaveable(saver = DCQLPresentationBuilderGraphViewNavigationManager.Saver) {
        DCQLPresentationBuilderGraphViewNavigationManager(listOf())
    }

    val selectionStack = viewModel.selectionStack
    BackHandler(selectionStack.isNotEmpty()) {
        viewModel.popSelection()
    }

    // for optional credential set queries, 0 represents "none" and all other indices need to subtract 1
    val selectedCredentialSetQueryOptions = selectionStack.toCredentialSetQueryOptions()
    val selectedSubmissionIndices = selectionStack.toSubmissionIndices()

    // separately tracking confirmations so we know which page to show
    val confirmedSelections = selectionStack.subList(
        0, selectionStack.lastIndexOf(DCQLPresentationBuilderGraphViewModelSelection.ContinueWithSelection) + 1
    )
    val confirmedCredentialSetQueryOptions = confirmedSelections.toCredentialSetQueryOptions()
    val confirmedSubmissionIndices = confirmedSelections.toSubmissionIndices()

    CommonPresentationPageScaffold(
        onClickLogo = onClickLogo,
        onClickSettings = onClickSettings,
        onNavigateUp = {
            viewModel.popSelectionsUntilConfirmationInclusive {
                onNavigateUp()
            }
        },
    ) {
        DCQLPresentationBuilderGraphViewContent(
            authenticateAtRelyingParty = authenticateAtRelyingParty,
            serviceProviderLocalizedLocation = serviceProviderLocalizedLocation,
            serviceProviderLocalizedName = serviceProviderLocalizedName,
            dcqlQuery = dcqlQuery,
            selectableCredentialSubmissionCards = selectableCredentialSubmissionCards,
            selectedCredentialSetQueryOptionIndices = selectedCredentialSetQueryOptions,
            selectedSubmissionIndices = selectedSubmissionIndices,
            confirmedCredentialSetQueryOptionIndices = confirmedCredentialSetQueryOptions,
            confirmedSubmissionIndices = confirmedSubmissionIndices,
            satisfiableCredentialQueries = satisfiableCredentialQueries,
            onSelectSubmissions = { unfulfilledRequestedCredentialQuery, selections ->
                viewModel.pushSelection(
                    DCQLPresentationBuilderGraphViewModelSelection.SelectSubmissions(
                        queryIdentifier = unfulfilledRequestedCredentialQuery,
                        submissionIndices = selections
                    )
                )
            },
            onSelectRequiredCredentialSetQueryOption = { credentialSetQueryIndex, optionIndex ->
                viewModel.pushSelection(
                    DCQLPresentationBuilderGraphViewModelSelection.SelectRequiredCredentialSetQueryOption(
                        credentialSetQueryIndex = credentialSetQueryIndex,
                        credentialSetQueryOptionIndex = optionIndex,
                    )
                )
            },
            onSelectOptionalCredentialSetQueryOption = { credentialSetQueryIndex, optionIndex ->
                viewModel.pushSelection(
                    DCQLPresentationBuilderGraphViewModelSelection.SelectOptionalCredentialSetQueryOption(
                        credentialSetQueryIndex = credentialSetQueryIndex,
                        credentialSetQueryOptionIndex = optionIndex,
                    )
                )
            },
            onContinueWithSelection = {
                viewModel.pushSelection(
                    DCQLPresentationBuilderGraphViewModelSelection.ContinueWithSelection
                )
            },
            onNavigateUp = {
                viewModel.popSelectionsUntilConfirmationInclusive {
                    onNavigateUp()
                }
            },
            onError = onError,
            onSubmit = {
                onSubmit(selectedSubmissionIndices)
            },
        )
    }
}

private fun List<DCQLPresentationBuilderGraphViewModelSelection>.toCredentialSetQueryOptions() =
    mutableMapOf<UInt, UInt>().apply {
        this@toCredentialSetQueryOptions.forEach {
            when (it) {
                DCQLPresentationBuilderGraphViewModelSelection.ContinueWithSelection,
                is DCQLPresentationBuilderGraphViewModelSelection.SelectSubmissions -> {
                    // noop
                }

                is DCQLPresentationBuilderGraphViewModelSelection.SelectRequiredCredentialSetQueryOption -> {
                    set(it.credentialSetQueryIndex, it.credentialSetQueryOptionIndex)
                }

                is DCQLPresentationBuilderGraphViewModelSelection.SelectOptionalCredentialSetQueryOption -> {
                    if (it.credentialSetQueryOptionIndex == null) {
                        remove(it.credentialSetQueryIndex)
                    } else {
                        set(it.credentialSetQueryIndex, it.credentialSetQueryOptionIndex)
                    }
                }
            }
        }
    }.toMap()

private fun List<DCQLPresentationBuilderGraphViewModelSelection>.toSubmissionIndices() =
    mutableMapOf<DCQLCredentialQueryIdentifier, Set<UInt>>().apply {
        this@toSubmissionIndices.forEach {
            when (it) {
                DCQLPresentationBuilderGraphViewModelSelection.ContinueWithSelection,
                is DCQLPresentationBuilderGraphViewModelSelection.SelectRequiredCredentialSetQueryOption,
                is DCQLPresentationBuilderGraphViewModelSelection.SelectOptionalCredentialSetQueryOption -> {
                    // noop
                }

                is DCQLPresentationBuilderGraphViewModelSelection.SelectSubmissions -> {
                    set(it.queryIdentifier, it.submissionIndices)
                }
            }
        }
    }.toMap()

private fun SnapshotStateList<DCQLPresentationBuilderGraphViewModelSelection>.popUntilConfirmation() {
    val lastContinue = lastIndexOf(DCQLPresentationBuilderGraphViewModelSelection.ContinueWithSelection)
    if (lastContinue >= 0) {
        removeRange(lastContinue, size)
    } else {
        clear()
    }
}

class DCQLPresentationBuilderGraphViewNavigationState(
    private var navigationStack: List<DCQLPresentationBuilderGraphViewNavigationAction>
) {
    val selectionStack: List<DCQLPresentationBuilderGraphViewModelSelection>
        get() = navigationStack.fold(
            emptyList<DCQLPresentationBuilderGraphViewModelSelection>()
        ) { privateSelectionStack, action ->
            when (action) {

                DCQLPresentationBuilderGraphViewNavigationAction.PopSelection ->
                    privateSelectionStack.subList(0, privateSelectionStack.lastIndex)

                DCQLPresentationBuilderGraphViewNavigationAction.PopSelectionsUntilConfirmationInclusive ->
                    privateSelectionStack.subList(
                        0,
                        privateSelectionStack.lastIndexOf(
                            DCQLPresentationBuilderGraphViewModelSelection.ContinueWithSelection
                        ).coerceAtLeast(0)
                    )

                is DCQLPresentationBuilderGraphViewNavigationAction.PushSelection ->
                    privateSelectionStack + action.action
            }
        }

    fun pushSelection(selection: DCQLPresentationBuilderGraphViewModelSelection) {
        navigationStack =
            if (
                navigationStack.lastOrNull()
                == DCQLPresentationBuilderGraphViewNavigationAction.PopSelectionsUntilConfirmationInclusive &&
                selection == DCQLPresentationBuilderGraphViewModelSelection.ContinueWithSelection
            ) {
                navigationStack.subList(0, navigationStack.lastIndex)
            } else {
                navigationStack + DCQLPresentationBuilderGraphViewNavigationAction.PushSelection(selection)
            }
    }

    fun popSelection() {
        navigationStack += DCQLPresentationBuilderGraphViewNavigationAction.PopSelection
    }

    fun popSelectionsUntilConfirmationInclusive() {
        navigationStack +=
            DCQLPresentationBuilderGraphViewNavigationAction.PopSelectionsUntilConfirmationInclusive
    }

    companion object {
        val Saver = listSaver(
            save = { it.navigationStack },
            restore = { DCQLPresentationBuilderGraphViewNavigationState(it) }
        )
    }
}