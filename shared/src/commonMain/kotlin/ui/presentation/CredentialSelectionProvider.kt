package ui.presentation

import at.asitplus.wallet.lib.agent.CredentialMatchingResult
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

internal fun <Credential : Any, Match : Any> hasMissingPresentationExchangeInputDescriptorMatches(
    inputDescriptorMatches: Map<String, Map<Credential, Match>>,
) = inputDescriptorMatches.values.any { it.isEmpty() }
