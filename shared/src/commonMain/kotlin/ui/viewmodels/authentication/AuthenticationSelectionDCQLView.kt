package ui.viewmodels.authentication

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import at.asitplus.openid.dcql.DCQLCredentialQueryIdentifier
import at.asitplus.openid.dcql.DCQLCredentialSubmissionOption
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.error_complex_dcql_query
import at.asitplus.valera.resources.error_unsatisfiable_dcql_query
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import org.jetbrains.compose.resources.stringResource
import ui.composables.DCQLCredentialQuerySubmissionSelection
import ui.state.savers.rememberMutableStateListOf
import ui.views.authentication.AuthenticationSelectionViewScaffold


@Composable
fun AuthenticationSelectionDCQLView(
    navigateUp: () -> Unit,
    onClickLogo: () -> Unit,
    confirmSelection: (CredentialPresentationSubmissions<SubjectCredentialStore.StoreEntry>?) -> Unit,
    matchingResult: DCQLMatchingResult<SubjectCredentialStore.StoreEntry>,
    decodeToBitmap: (ByteArray) -> ImageBitmap?,
    onError: (Throwable) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (matchingResult.presentationRequest.dcqlQuery.requestedCredentialSetQueries.size != 1) {
        return onError(UnsupportedOperationException(stringResource(Res.string.error_complex_dcql_query)))
    }
    val credentialSetQuery = matchingResult.presentationRequest.dcqlQuery.requestedCredentialSetQueries.first()

    if (credentialSetQuery.options.size != 1) {
        return onError(UnsupportedOperationException(stringResource(Res.string.error_complex_dcql_query)))
    }
    val submissionOptions = credentialSetQuery.options.first().associateWith { credentialQueryIdentifier ->
        matchingResult.dcqlQueryResult.credentialQueryMatches[credentialQueryIdentifier]?.ifEmpty { null }
            ?: return onError(IllegalArgumentException(stringResource(Res.string.error_unsatisfiable_dcql_query)))
    }

    AuthenticationSelectionDCQLView(
        navigateUp = navigateUp,
        onClickLogo = onClickLogo,
        decodeToBitmap = decodeToBitmap,
        confirmSelection = confirmSelection,
        credentialQueryOptions = submissionOptions,
        onError = onError,
        modifier = modifier,
    )
}

@Composable
fun AuthenticationSelectionDCQLView(
    navigateUp: () -> Unit,
    onClickLogo: () -> Unit,
    decodeToBitmap: (ByteArray) -> ImageBitmap?,
    confirmSelection: (CredentialPresentationSubmissions<SubjectCredentialStore.StoreEntry>?) -> Unit,
    credentialQueryOptions: Map<DCQLCredentialQueryIdentifier, List<DCQLCredentialSubmissionOption<SubjectCredentialStore.StoreEntry>>>,
    onError: (Throwable) -> Unit,
    modifier: Modifier = Modifier,
) {
    val orderedCredentialQueryIdentifiers = rememberSaveable(credentialQueryOptions) {
        credentialQueryOptions.keys.toList().map {
            it.string
        }
    }
    var currentCredentialQueryIdentifierIndex by rememberSaveable(orderedCredentialQueryIdentifiers) {
        mutableStateOf(0)
    }

    val selectedOptions = rememberMutableStateListOf {
        mutableStateListOf(
            *List<Int?>(orderedCredentialQueryIdentifiers.size) { null }.toTypedArray()
        )
    }

    val currentlyAvailableOptions = remember(currentCredentialQueryIdentifierIndex) {
        credentialQueryOptions[
            DCQLCredentialQueryIdentifier(
                orderedCredentialQueryIdentifiers[currentCredentialQueryIdentifierIndex]
            )
        ] ?: run {
            onError(IllegalStateException())
            listOf()
        }
    }

    var currentlySelectedOptionIndex by rememberSaveable(currentCredentialQueryIdentifierIndex) {
        mutableStateOf(selectedOptions[currentCredentialQueryIdentifierIndex])
    }

    AuthenticationSelectionViewScaffold(
        onNavigateUp = navigateUp,
        onClickLogo = onClickLogo,
        onNext = {
            selectedOptions[currentCredentialQueryIdentifierIndex] = currentlySelectedOptionIndex
            val nextUnselected = selectedOptions.indexOfFirst {
                it == null
            }
            if (nextUnselected == -1) {
                confirmSelection(
                    DCQLCredentialSubmissions(
                        credentialQuerySubmissions = selectedOptions.mapIndexed { index, it ->
                            val id = DCQLCredentialQueryIdentifier(
                                orderedCredentialQueryIdentifiers[index]
                            )
                            id to credentialQueryOptions[id]!![it!!]
                        }.toMap()
                    )
                )
            } else {
                currentCredentialQueryIdentifierIndex = nextUnselected
            }
        },
        modifier = modifier,
    ) {
        LinearProgressIndicator(
            progress = { ((1.0f / credentialQueryOptions.size) * (currentCredentialQueryIdentifierIndex + 1)) },
            modifier = Modifier.fillMaxWidth(),
            drawStopIndicator = { }
        )
        DCQLCredentialQuerySubmissionSelection(
            selectionOptions = currentlyAvailableOptions,
            onChangeSelection = { currentlySelectedOptionIndex = it },
            decodeToBitmap = decodeToBitmap,
            currentlySelectedOptionIndex = currentlySelectedOptionIndex,
            modifier = Modifier.fillMaxSize().verticalScroll(state = rememberScrollState())
                .padding(16.dp),
        )
    }
}