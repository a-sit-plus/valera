package ui.presentation

import at.asitplus.wallet.lib.agent.CredentialMatchingResult
import at.asitplus.wallet.lib.agent.HolderIsoDeviceRetrievalQueryMatchingResult
import at.asitplus.wallet.lib.agent.validation.CredentialFreshnessSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import ui.models.CredentialFreshnessValidationStateUiModel
import ui.models.CredentialFreshnessValidationStateUiModel.Done
import ui.models.CredentialFreshnessValidationStateUiModel.Loading
import ui.models.toCredentialFreshnessSummaryModel

data class CredentialSelectionProvider<Credential : Any>(
    val queryMatchingResult: CredentialMatchingResult<Credential>,
    val credentialFreshnessProviders: List<StateFlow<CredentialFreshnessValidationStateUiModel>>
)

fun <Credential : Any> CredentialMatchingResult<Credential>.toCredentialSelectionProvider(
    scope: CoroutineScope,
    checkCredentialFreshness: suspend (Credential) -> CredentialFreshnessSummary,
) = CredentialSelectionProvider(
    queryMatchingResult = this,
    credentialFreshnessProviders = this.matchingResult.credentials.map {
        flow {
            emit(Loading)
            emit(Done(checkCredentialFreshness(it).toCredentialFreshnessSummaryModel()))
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(60_000),
            initialValue = Loading,
        )
    }
)

/**
 * True when the request cannot be answered at all: at least one requested document has no matching credential, or
 * the Device Request asked for no document in the first place. Both must route to the no-credential screen; a
 * request with no documents would otherwise reach a selection step that has nothing to select.
 */
internal fun HolderIsoDeviceRetrievalQueryMatchingResult<*>.hasUnsatisfiedDocumentRequest() =
    documentMatches.isEmpty() || documentMatches.any { it.isEmpty() }
