package ui.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import at.asitplus.data.NonEmptyList.Companion.toNonEmptyList
import at.asitplus.openid.dcql.DCQLCredentialQueryIdentifier
import at.asitplus.openid.dcql.DCQLQuery
import at.asitplus.wallet.app.common.extractConsentData
import at.asitplus.wallet.app.common.toCredentialQueryUiModel

/**
 * This composable displays an appropriate presentation selection view depending on current selections.
 * The primary goal is to have a simple UI for the user in simple cases - i.e. only ask for credential if there are no
 * credential sets.
 * The secondary goal is to properly deal with the case where a credential query is present in two credential sets.
 *  -> We can only select one credential per credential query, so we probably shouldn't ask the user a second time.
 */
@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@Composable
fun DCQLPresentationBuilderGraphViewContent(
    authenticateAtRelyingParty: Boolean,
    serviceProviderLocalizedName: String?,
    serviceProviderLocalizedLocation: String,
    dcqlQuery: DCQLQuery,
    /**
     * This delegates to library users how to display the credential cards.
     * A library user may also include credentials that did not match and simply not invoke the selection function.
     */
    selectableCredentialSubmissionCards: Map<DCQLCredentialQueryIdentifier, List<SelectableCredentialSubmissionCard>>,
    satisfiableCredentialQueries: Collection<DCQLCredentialQueryIdentifier>,
    onNavigateUp: () -> Unit,
    onError: (Throwable) -> Unit,
    onContinueWithSelection: () -> Unit,
    onSelectSubmissions: (DCQLCredentialQueryIdentifier, Set<UInt>) -> Unit,
    onSelectRequiredCredentialSetQueryOption: (UInt, UInt) -> Unit,
    onSelectOptionalCredentialSetQueryOption: (UInt, UInt?) -> Unit,
    selectedSubmissionIndices: Map<DCQLCredentialQueryIdentifier, Set<UInt>>,
    selectedRequiredCredentialSetQueryOptions: Map<UInt, UInt?>,
    confirmedRequiredCredentialSetQueryOptions: Map<UInt, UInt>,
    confirmedSubmissionIndices: Map<DCQLCredentialQueryIdentifier, Set<UInt>>,
    onSubmit: () -> Unit,
    selectedOptionalCredentialSetQueryOptions: Map<UInt, UInt?>,
    confirmedOptionalCredentialSetQueryOptions: Map<UInt, UInt?>,
) {
    val credentialSetQueries = dcqlQuery.requestedCredentialSetQueries
    val progressStart = 1
    val progressCredentialSetQueries = credentialSetQueries.count {
        it.options.any {
            it.all {
                confirmedSubmissionIndices[it]?.isNotEmpty() == true
            }
        }
    }
    val totalRequiredCredentialSetQueries = credentialSetQueries.size
    val currentProgress = progressStart + progressCredentialSetQueries
    val stepsToBeDone =
        progressStart + totalRequiredCredentialSetQueries + 1 // one step for final submission or selecting optionals

    LinearProgressIndicator(
        progress = { currentProgress.toFloat() / stepsToBeDone },
        modifier = Modifier.fillMaxWidth(),
    )

    val requiredCredentialQueries = credentialSetQueries.mapIndexedNotNull { index, it ->
        it.takeIf {
            it.required
        }?.options?.reduce { acc, it ->
            acc.intersect(it).toList()
        }
    }.flatten()

    // credentials that are required or chosen through selected credential set queries
    val requestedCredentialQueries = requiredCredentialQueries + credentialSetQueries.mapIndexedNotNull { index, it ->
        confirmedRequiredCredentialSetQueryOptions[index.toUInt()]?.let { selectedOptionIndex ->
            it.options.getOrNull(selectedOptionIndex.toInt())
        } ?: confirmedOptionalCredentialSetQueryOptions[index.toUInt()]?.let { selectedOptionIndex ->
            it.options.getOrNull(selectedOptionIndex.toInt())
        } ?: listOf()
    }.flatten()

    val credentialQueryUiModels = dcqlQuery.credentials.associate {
        it.id to try { // TODO: improve on this by storing it somewhere?
            it.extractConsentData()
        } catch (e: Throwable) {
            return LaunchedEffect(Unit) {
                onError(e)
            }
        }.toCredentialQueryUiModel()
    }

    requestedCredentialQueries.firstOrNull {
        confirmedSubmissionIndices[it]?.isEmpty() ?: true
    }?.let { unconfirmedRequestedCredentialQuery ->
        val credentialQuery = dcqlQuery.credentials.firstOrNull {
            it.id == unconfirmedRequestedCredentialQuery
        } ?: return LaunchedEffect(Unit) {
            onError(IllegalStateException("Credential query with id `$unconfirmedRequestedCredentialQuery` not found in request $dcqlQuery"))
        }
        val credentialQueryUiModel = credentialQueryUiModels[credentialQuery.id] ?: return LaunchedEffect(Unit) {
            onError(IllegalStateException("Credential query model for query with id `$unconfirmedRequestedCredentialQuery` not found."))
        }
        val selectedIndices = selectedSubmissionIndices[unconfirmedRequestedCredentialQuery] ?: setOf()

        return AuthenticationCredentialQueryCredentialSelection(
            credentialQueryUiModel = credentialQueryUiModel,
            allowMultiSelection = credentialQuery.multiple,
            selectableCredentialSubmissionCards = selectableCredentialSubmissionCards[unconfirmedRequestedCredentialQuery] ?: listOf(),
            onAbort = onNavigateUp,
            onContinue = onContinueWithSelection.takeIf {
                selectedIndices.isNotEmpty()
            },
            selectedIndices = selectedIndices,
            onSelectIndices = {
                onSelectSubmissions(
                    unconfirmedRequestedCredentialQuery,
                    it,
                )
            }
        )
    }

    credentialSetQueries.withIndex().firstOrNull { (_, it) ->
        it.required && it.options.all {
            it.any {
                confirmedSubmissionIndices[it]?.isNotEmpty() != true
            }
        }
    }?.let { unsatisfiedRequiredCredentialSetQuery ->
        val missingQueriesPerOption = unsatisfiedRequiredCredentialSetQuery.value.options.map {
            it - confirmedSubmissionIndices.filter {
                it.value.isNotEmpty()
            }.keys
        }.toNonEmptyList()

        val selectedOptionIndex =
            selectedRequiredCredentialSetQueryOptions[unsatisfiedRequiredCredentialSetQuery.index.toUInt()]

        return DCQLPresentationRequiredCredentialSetQueryOptionSelectionPageContent(
            credentialSetQueryOptionUiModels = missingQueriesPerOption.map {
                CredentialSetQueryOptionUiModel(
                    isSatisfiable = it.all {
                        it in satisfiableCredentialQueries
                    },
                    credentialQueries = it.map { identifier ->
                        credentialQueryUiModels[identifier] ?: return LaunchedEffect(Unit) {
                            onError(
                                IllegalStateException("Unable to find credential query ui model for id `$identifier`")
                            )
                        }
                    }
                )
            }.toNonEmptyList(),
            onAbort = onNavigateUp,
            onContinue = onContinueWithSelection.takeIf {
                selectedOptionIndex != null
            },
            selectedOptionIndex = selectedOptionIndex,
            onSetSelectedOptionIndex = {
                onSelectRequiredCredentialSetQueryOption(
                    unsatisfiedRequiredCredentialSetQuery.index.toUInt(),
                    it,
                )
            }
        )
    }

    credentialSetQueries.withIndex().firstOrNull { (index, it) ->
        !it.required && it.options.all {
            it.any {
                it !in confirmedSubmissionIndices
            }
        } && index.toUInt() !in confirmedOptionalCredentialSetQueryOptions
    }?.let { nonSelectedOptionalCredentialSetQuery ->
        val missingQueriesPerOption = nonSelectedOptionalCredentialSetQuery.value.options.map {
            it - confirmedSubmissionIndices.filter {
                it.value.isNotEmpty()
            }.keys
        }

        val isAnySelected = nonSelectedOptionalCredentialSetQuery.index.toUInt() in selectedOptionalCredentialSetQueryOptions
        val selectedOptionIndex = selectedOptionalCredentialSetQueryOptions[nonSelectedOptionalCredentialSetQuery.index.toUInt()]

        return DCQLPresentationOptionalCredentialSetQueryOptionSelectionPageContent(
            credentialSetQueryOptionUiModels = missingQueriesPerOption.map {
                CredentialSetQueryOptionUiModel(
                    isSatisfiable = it.all {
                        it in satisfiableCredentialQueries
                    },
                    credentialQueries = it.map { identifier ->
                        credentialQueryUiModels[identifier] ?: return LaunchedEffect(Unit) {
                            onError(
                                IllegalStateException("Unable to find credential query ui model for id `$identifier`")
                            )
                        }
                    }
                )
            }.toNonEmptyList(),
            onAbort = onNavigateUp,
            onContinue = onContinueWithSelection,
            selectedOptionIndex = selectedOptionIndex,
            onSetSelectedOptionIndex = {
                onSelectOptionalCredentialSetQueryOption(
                    nonSelectedOptionalCredentialSetQuery.index.toUInt(),
                    it,
                )
            },
            isAnySelected = isAnySelected,
        )
    }

    DCQLPresentationFinalizationPageContent(
        authenticateAtRelyingParty = authenticateAtRelyingParty,
        serviceProviderLocalizedName = serviceProviderLocalizedName,
        serviceProviderLocalizedLocation = serviceProviderLocalizedLocation,
        dcqlQuery = dcqlQuery,
        selections = selectableCredentialSubmissionCards.mapValues { (id, submissionCards) ->
            confirmedSubmissionIndices[id]?.map {
                submissionCards[it.toInt()]
            } ?: listOf()
        },
        onAbort = onNavigateUp,
        onSubmit = onSubmit
    )
}

