package ui.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import at.asitplus.openid.dcql.DCQLCredentialQueryIdentifier
import at.asitplus.openid.dcql.DCQLQuery
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.attribute_friendly_name_data_recipient_location
import at.asitplus.valera.resources.attribute_friendly_name_data_recipient_name
import at.asitplus.valera.resources.heading_label_authenticate_at_device_screen
import at.asitplus.valera.resources.heading_label_show_data_third_party
import at.asitplus.valera.resources.prompt_send_above_data
import at.asitplus.valera.resources.section_heading_data_recipient
import at.asitplus.wallet.app.common.extractConsentData
import at.asitplus.wallet.app.common.toCredentialQueryUiModel
import org.jetbrains.compose.resources.stringResource
import ui.composables.DataDisplaySection
import ui.composables.ScreenHeading

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
    onSelectSubmissions: (DCQLCredentialQueryIdentifier, List<UInt>) -> Unit,
    onSelectRequiredCredentialSetQueryOption: (UInt, UInt) -> Unit,
    onSelectOptionalCredentialSetQueryOption: (UInt, UInt?) -> Unit,
    selectedSubmissionIndices: Map<DCQLCredentialQueryIdentifier, List<UInt>>,
    selectedCredentialSetQueryOptionIndices: Map<UInt, UInt>,
    onSubmit: () -> Unit,
) {
    val credentialSetQueries = dcqlQuery.requestedCredentialSetQueries
    val progressStart = 1
    val progressRequiredCredentialSetQueries = credentialSetQueries.count {
        it.required && it.options.any {
            it.all {
                selectedSubmissionIndices[it]?.isNotEmpty() == true
            }
        }
    }
    val totalRequiredCredentialSetQueries = credentialSetQueries.count {
        it.required
    }
    val currentProgress = progressStart + progressRequiredCredentialSetQueries
    val stepsToBeDone =
        progressStart + totalRequiredCredentialSetQueries + 1 // one step for final submission or selection optionals

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
        selectedCredentialSetQueryOptionIndices[index.toUInt()]?.let { selectedOptionIndex ->
            it.options.getOrNull(selectedOptionIndex.toInt())
        } ?: listOf()
    }.flatten()

    requestedCredentialQueries.firstOrNull {
        selectedSubmissionIndices[it]?.isEmpty() ?: true
    }?.let { unfulfilledRequestedCredentialQuery ->
        val credentialQuery = dcqlQuery.credentials.firstOrNull {
            it.id == unfulfilledRequestedCredentialQuery
        } ?: return LaunchedEffect(Unit) {
            onError(IllegalStateException("Credential query with id `$unfulfilledRequestedCredentialQuery` not found in request $dcqlQuery"))
        }

        return AuthenticationCredentialQueryCredentialSelection(
            credentialQuery = credentialQuery,
            selectableCredentialSubmissionCards = selectableCredentialSubmissionCards[unfulfilledRequestedCredentialQuery],
            onAbort = onNavigateUp,
            onContinue = { selections ->
                onSelectSubmissions(
                    unfulfilledRequestedCredentialQuery,
                    selections,
                )
            }
        )
    }

    val credentialQueries = dcqlQuery.credentials.associate {
        it.id to try { // TODO: improve on this by storing it somewhere?
            it.extractConsentData()
        } catch (e: Throwable) {
            return LaunchedEffect(Unit) {
                onError(e)
            }
        }.toCredentialQueryUiModel()
    }

    credentialSetQueries.withIndex().firstOrNull { (_, it) ->
        it.required && it.options.all {
            it.any {
                selectedSubmissionIndices[it]?.isNotEmpty() != true
            }
        }
    }?.let { unsatisfiedRequiredCredentialSetQuery ->
        val missingQueriesPerOption = unsatisfiedRequiredCredentialSetQuery.value.options.map {
            it - selectedSubmissionIndices.filter {
                it.value.isNotEmpty()
            }.keys
        }

        return DCQLPresentationRequiredCredentialSetQueryOptionSelectionPageContent(
            credentialSetQueryOptionUiModels = missingQueriesPerOption.map {
                CredentialSetQueryOptionUiModel(
                    isSatisfiable = it.all {
                        it in satisfiableCredentialQueries
                    },
                    credentialQueries = it.map { identifier ->
                        credentialQueries[identifier] ?: return LaunchedEffect(Unit) {
                            onError(
                                IllegalStateException("Unable to find credential query ui model for id `$identifier`")
                            )
                        }
                    }
                )
            },
            onAbort = onNavigateUp,
            onContinue = {
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
                selectedSubmissionIndices[it]?.isNotEmpty() != true
            }
        } && index.toUInt() !in selectedCredentialSetQueryOptionIndices.keys
    }?.let { nonSelectedOptionalCredentialSetQuery ->
        val missingQueriesPerOption = nonSelectedOptionalCredentialSetQuery.value.options.map {
            it - selectedSubmissionIndices.filter {
                it.value.isNotEmpty()
            }.keys
        }

        return DCQLPresentationOptionalCredentialSetQueryOptionSelectionPageContent(
            credentialSetQueryOptionUiModels = missingQueriesPerOption.map {
                CredentialSetQueryOptionUiModel(
                    isSatisfiable = it.all {
                        it in satisfiableCredentialQueries
                    },
                    credentialQueries = it.map { identifier ->
                        credentialQueries[identifier] ?: return LaunchedEffect(Unit) {
                            onError(
                                IllegalStateException("Unable to find credential query ui model for id `$identifier`")
                            )
                        }
                    }
                )
            },
            onAbort = onNavigateUp,
            onContinue = {
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
            selectedSubmissionIndices[id]?.map {
                submissionCards[it.toInt()]
            } ?: listOf()
        },
        onAbort = onNavigateUp,
        onSubmit = onSubmit
    )

    // all required credential set queries are satisfied and optional queries have been asked for
    // TODO: show summary?
//
//    matchingResult.presentationRequest.dcqlQuery.credentials.map {
//        it.id
//    }.size
//
//    val requiredCredentialSetQueries = credentialSetQueries.filter {
//        it.required
//    }
//    val optionalCredentialSetQueries = credentialSetQueries.filter {
//        !it.required
//    }
//
//    // TODO: iterate through credential set queries and select credentials as applicable
//    val requiredCredentialSetQueryIndex by rememberSaveable {
//        mutableStateOf(0)
//    }
//    val optionalCredentialSetQueryIndex by rememberSaveable {
//        mutableStateOf(0)
//    }
//    val progress = 1 + requiredCredentialSetQueryIndex + optionalCredentialSetQueryIndex
//    val totalQueries = 1 + credentialSetQueries.size + 1
//    when {
//        requiredCredentialSetQueryIndex < requiredCredentialSetQueries.size -> {
//            val globalCredentialSetQueryIndex = credentialSetQueries.mapIndexed { index, query ->
//                index.takeIf {
//                    query.required
//                }
//            }.filterNotNull().getOrNull(requiredCredentialSetQueryIndex) ?: return onAbort(
//                IllegalStateException( // TODO: translate?
//                    "Expected ${requiredCredentialSetQueryIndex + 1} required credential set queries to exist, but only found ${
//                        credentialSetQueries.count {
//                            it.required
//                        }
//                    }."
//                )
//            )
//
//            DCQLPresentationBuilderCredentialSetQueryOptionSelectionGraphView(
//                alreadySelectedSubmissions = viewModel.selectedSubmissionIndices.filter {
//                    it.value.isNotEmpty()
//                }.keys,
//                isCredentialSetQueryRequired = true,
//                credentialSetQueryOptionUiModels = listOf(),
//                onAbort = onAbort,
//                onSelectCredentialQuerySetOption = {
//                    viewModel.selectedCredentialSetQueryOptions[globalCredentialSetQueryIndex] = it
//                },
//                onSelectSubmissions = {
//                    selectedOptionIndexFixed?.let {
//                        onContinue(it)
//
//                    }
//                },
//                onDone = {
//                    selectedOptionIndexFixed?.let {
//                        onContinue(it)
//
//                    }
//                },
//                onSelectCredentialSetQueryOptionAtIndex = {
//                    viewModel.selectedCredentialSetQueryOptions[globalCredentialSetQueryIndex] = it
//                }
//            )
//        }
//
//        requiredCredentialSetQueryIndex < requiredCredentialSetQueries.size -> AuthenticationCredentialSetQueryOptionSelectionGraphView(
//            isCredentialSetQueryRequired = true,
//            totalCredentialSetQueries = totalQueries.toUInt(),
//            currentCredentialSetQueryIndexPlus1 = requiredCredentialSetQueryIndex.toUInt() + 1u,
//            credentialSetQueryOptionUiModels = listOf(),
//            onAbort = onAbort,
//            onContinue = {
//
//            }
//        )
//
//        optionalCredentialSetQueryIndex < optionalCredentialSetQueries.size -> DCQLPresentationCredentialSetQueryOptionSelectionPageContent(
//            isCredentialSetQueryRequired = false,
//            totalCredentialSetQueries = totalQueries.toUInt(),
//            currentCredentialSetQueryIndexPlus1 = requiredCredentialSetQueryIndex.toUInt() + 1u,
//            selectedOptionIndex = null,
//            credentialSetQueryOptionUiModels = listOf(),
//            onSelectCredentialSetQueryOptionAtIndex = {
//
//            },
//            onAbort = onAbort,
//            onContinue = {
//
//            }
//        )
//
//        else -> {
//            // submit?
//        }
//    }


//    val relevantCredentials = matchingResult.dcqlQueryResult.credentialQueryMatches.values.flatMap {
//        it.map {
//            it.credential
//        }
//    }.map {
//        val credentialFreshnessValidationState by produceState(
//            CredentialFreshnessValidationStateUiModel.Loading as CredentialFreshnessValidationStateUiModel,
//            it,
//        ) {
//            value = CredentialFreshnessValidationStateUiModel.Loading
//            value = CredentialFreshnessValidationStateUiModel.Done(
//                viewModel.credentialFreshnessSummary(it)
//            )
//        }
//    }
}

@Composable
fun DCQLPresentationFinalizationPageContent(
    authenticateAtRelyingParty: Boolean,
    serviceProviderLocalizedName: String?,
    serviceProviderLocalizedLocation: String,
    dcqlQuery: DCQLQuery,
    selections: Map<DCQLCredentialQueryIdentifier, List<SelectableCredentialSubmissionCard>>,
    onAbort: () -> Unit,
    onSubmit: () -> Unit,
    serviceProviderLogo: ImageBitmap? = null,
) {
    Scaffold(
        bottomBar = {
            CommonBottomButtonsAbortContinue(
                text = stringResource(Res.string.prompt_send_above_data),
                onAbort = onAbort,
                onContinue = onSubmit,
            )
        }
    ) {
        Column(
            modifier = Modifier.padding(it).fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            val title = if (authenticateAtRelyingParty) {
                stringResource(Res.string.heading_label_authenticate_at_device_screen)
            } else {
                stringResource(Res.string.heading_label_show_data_third_party)
            }
            ScreenHeading(title)

            if (serviceProviderLogo != null) {
                Box(Modifier.Companion.fillMaxWidth(), contentAlignment = Alignment.Companion.Center) {
                    Image(
                        bitmap = serviceProviderLogo,
                        contentDescription = null,
                        contentScale = ContentScale.Companion.Fit,
                        modifier = Modifier.Companion.height(64.dp),
                    )
                }
            }

            DataDisplaySection(
                title = stringResource(Res.string.section_heading_data_recipient),
                data = listOfNotNull(
                    serviceProviderLocalizedName?.let {
                        stringResource(Res.string.attribute_friendly_name_data_recipient_name) to serviceProviderLocalizedName
                    },
                    stringResource(Res.string.attribute_friendly_name_data_recipient_location) to serviceProviderLocalizedLocation,
                ),
            )

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(state = rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                selections.entries.sortedBy {
                    it.key.string
                }.flatMap {
                    it.value
                }.forEach { card ->
                    // TODO: good enough or should we have separate cards for final submissions?
                    //  - if these cards should be reused, then allowMultiSelection shouldn't be relevant with (isSelected, onToggleSelection) = (true, null)
                    //  - Cards should therefore implicitly handle the case (true, *, null) to show the card without any selection specific semantics UI
                    card(
                        isSelected = true,
                        allowMultiSelection = false,
                        onToggleSelection = null
                    )
                }
            }
        }
    }
}