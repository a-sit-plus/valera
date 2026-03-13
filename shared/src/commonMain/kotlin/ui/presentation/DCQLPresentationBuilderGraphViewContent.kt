package ui.presentation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import at.asitplus.data.NonEmptyList.Companion.toNonEmptyList
import at.asitplus.openid.dcql.DCQLCredentialQueryIdentifier
import at.asitplus.openid.dcql.DCQLQuery
import at.asitplus.wallet.app.common.extractConsentData
import at.asitplus.wallet.app.common.toCredentialQueryUiModel

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
    selectedCredentialSetQueryOptionIndices: Map<UInt, UInt>,
    confirmedCredentialSetQueryOptionIndices: Map<UInt, UInt>,
    confirmedSubmissionIndices: Map<DCQLCredentialQueryIdentifier, Set<UInt>>,
    onSubmit: () -> Unit,
) {
    val credentialSetQueries = dcqlQuery.requestedCredentialSetQueries
    val progressStart = 1
    val progressRequiredCredentialSetQueries = credentialSetQueries.count {
        it.required && it.options.any {
            it.all {
                confirmedSubmissionIndices[it]?.isNotEmpty() == true
            }
        }
    }
    val totalRequiredCredentialSetQueries = credentialSetQueries.count {
        it.required
    }
    val currentProgress = progressStart + progressRequiredCredentialSetQueries
    val stepsToBeDone =
        progressStart + totalRequiredCredentialSetQueries + 1 // one step for final submission or selecting optionals

    val requiredCredentialQueries = credentialSetQueries.mapIndexedNotNull { index, it ->
        it.takeIf {
            it.required
        }?.options?.reduce { acc, it ->
            acc.intersect(it).toList()
        }
    }.flatten()

    // TODO: use as discriminator for showing dcql credential query details so user knows for which query the submission is selected?
    //  - alternative: always show for showing dcql credential query details so user knows for which query the submission is selected?
    val isAnyCredentialQueryOptional = dcqlQuery.credentials.map {
        it.id
    }.any {
        it !in requiredCredentialQueries
    }

    LinearProgressIndicator(
        progress = { currentProgress.toFloat() / stepsToBeDone }
    )

    // credentials that are required or chosen through selected credential set queries
    val requestedCredentialQueries = requiredCredentialQueries + credentialSetQueries.mapIndexedNotNull { index, it ->
        confirmedCredentialSetQueryOptionIndices[index.toUInt()]?.let { selectedOptionIndex ->
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
            selectedCredentialSetQueryOptionIndices[unsatisfiedRequiredCredentialSetQuery.index.toUInt()]

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
                confirmedSubmissionIndices[it]?.isNotEmpty() != true
            }
        } && index.toUInt() !in selectedCredentialSetQueryOptionIndices.keys
    }?.let { nonSelectedOptionalCredentialSetQuery ->
        val missingQueriesPerOption = nonSelectedOptionalCredentialSetQuery.value.options.map {
            it - confirmedSubmissionIndices.filter {
                it.value.isNotEmpty()
            }.keys
        }
        val selectedOptionIndex =
            selectedCredentialSetQueryOptionIndices[nonSelectedOptionalCredentialSetQuery.index.toUInt()]

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
            onContinue = onContinueWithSelection.takeIf {
                selectedOptionIndex != null
            },
            selectedOptionIndex = selectedOptionIndex,
            onSetSelectedOptionIndex = {
                onSelectOptionalCredentialSetQueryOption(
                    nonSelectedOptionalCredentialSetQuery.index.toUInt(),
                    it,
                )
            }
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

