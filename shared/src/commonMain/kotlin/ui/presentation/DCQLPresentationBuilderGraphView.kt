package ui.presentation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import at.asitplus.catchingUnwrapped
import at.asitplus.openid.dcql.DCQLCredentialQueryIdentifier
import at.asitplus.openid.dcql.DCQLQuery
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.biometric_authentication_prompt_for_data_transmission_consent_title
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.CredentialPresentation
import at.asitplus.wallet.lib.data.CredentialPresentationRequest
import at.asitplus.wallet.lib.ktor.openid.OpenId4VpWallet
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.compose.viewmodel.koinViewModel
import ui.viewmodels.authentication.CredentialPresentationSubmissions
import ui.viewmodels.authentication.DCQLCredentialSubmissions
import ui.viewmodels.authentication.PresentationExchangeCredentialSubmissions

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
    selectableCredentialSubmissionCards: Map<DCQLCredentialQueryIdentifier, List<SelectableCredentialSubmissionCard>>,
    satisfiableCredentialQueries: Collection<DCQLCredentialQueryIdentifier>,
    viewModel: DCQLPresentationBuilderGraphViewModel = koinViewModel(),
    onError: (Throwable) -> Unit,
    onNavigateUp: () -> Unit,
    onSubmit: (Map<DCQLCredentialQueryIdentifier, List<UInt>>) -> Unit,
) {
    BackHandler(viewModel.selectionStack.isNotEmpty()) {
        viewModel.selectionStack.removeLast()
    }

    // for optional credential set queries, 0 represents "none" and all other indices need to subtract 1
    val selectedCredentialSetQueryOptions = mutableMapOf<UInt, UInt>().apply {
        viewModel.selectionStack.forEach {
            when (it) {
                is DCQLPresentationBuilderGraphViewModelAction.SelectRequiredCredentialSetQueryOption -> {
                    set(it.credentialSetQueryIndex, it.credentialSetQueryOptionIndex)
                }

                is DCQLPresentationBuilderGraphViewModelAction.SelectOptionalCredentialSetQueryOption -> {
                    if (it.credentialSetQueryOptionIndex == null) {
                        remove(it.credentialSetQueryIndex)
                    } else {
                        set(it.credentialSetQueryIndex, it.credentialSetQueryOptionIndex)
                    }
                }

                is DCQLPresentationBuilderGraphViewModelAction.SelectSubmissions -> {
                    // noop
                }
            }
        }
    }.toMap()

    val selectedSubmissionIndices = mutableMapOf<DCQLCredentialQueryIdentifier, List<UInt>>().apply {
        viewModel.selectionStack.forEach {
            when (it) {
                is DCQLPresentationBuilderGraphViewModelAction.SelectRequiredCredentialSetQueryOption,
                is DCQLPresentationBuilderGraphViewModelAction.SelectOptionalCredentialSetQueryOption -> {
                    // noop
                }

                is DCQLPresentationBuilderGraphViewModelAction.SelectSubmissions -> {
                    set(it.queryIdentifier, it.submissionIndices)
                }
            }
        }
    }.toMap()

    CommonPresentationPageScaffold(
        onClickLogo = onClickLogo,
        onClickSettings = onClickSettings,
        onNavigateUp = {
            if (viewModel.selectionStack.isEmpty()) {
                onNavigateUp()
            } else {
                viewModel.selectionStack.removeLast()
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
            satisfiableCredentialQueries = satisfiableCredentialQueries,
            onSelectSubmissions = { unfulfilledRequestedCredentialQuery, selections ->
                viewModel.selectionStack.add(
                    DCQLPresentationBuilderGraphViewModelAction.SelectSubmissions(
                        queryIdentifier = unfulfilledRequestedCredentialQuery,
                        submissionIndices = selections
                    )
                )
            },
            onSelectRequiredCredentialSetQueryOption = { credentialSetQueryIndex, optionIndex ->
                viewModel.selectionStack.add(
                    DCQLPresentationBuilderGraphViewModelAction.SelectRequiredCredentialSetQueryOption(
                        credentialSetQueryIndex = credentialSetQueryIndex,
                        credentialSetQueryOptionIndex = optionIndex,
                    )
                )
            },
            onSelectOptionalCredentialSetQueryOption = { credentialSetQueryIndex, optionIndex ->
                viewModel.selectionStack.add(
                    DCQLPresentationBuilderGraphViewModelAction.SelectOptionalCredentialSetQueryOption(
                        credentialSetQueryIndex = credentialSetQueryIndex,
                        credentialSetQueryOptionIndex = optionIndex,
                    )
                )
            },
            onNavigateUp = {
                if (viewModel.selectionStack.isEmpty()) {
                    onNavigateUp()
                } else {
                    viewModel.selectionStack.removeLast()
                }
            },
            onError = onError,
            onSubmit = {
                onSubmit(selectedSubmissionIndices)
            }
        )
    }
}

