package ui.presentation

import at.asitplus.wallet.lib.agent.validation.CredentialFreshnessSummary
import at.asitplus.wallet.lib.openid.CredentialMatchingResult
import at.asitplus.wallet.lib.openid.PresentationExchangeMatchingResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import ui.models.CredentialFreshnessSummaryUiModel
import ui.models.CredentialFreshnessValidationStateUiModel
import ui.models.toCredentialFreshnessSummaryModel

data class CredentialSelectionProvider<Credential : Any>(
    val queryMatchingResult: CredentialMatchingResult<Credential>,
    val credentialFreshnessProviders: List<StateFlow<CredentialFreshnessValidationStateUiModel>>
)

fun <Credential : Any> CredentialMatchingResult<Credential>.toCredentialSelectionProvider(
    scope: CoroutineScope,
    checkCredentialFreshness: suspend (Credential) -> CredentialFreshnessSummary,
) = CredentialSelectionProvider(
    queryMatchingResult = this.also {
        if (it is PresentationExchangeMatchingResult) {
            validatePresentationExchangeInputDescriptorMatches(it.matchingResult.inputDescriptorMatches)
        }
    },
    credentialFreshnessProviders = this.matchingResult.credentials.map {
        flow {
            emit(CredentialFreshnessValidationStateUiModel.Loading)
            emit(
                CredentialFreshnessValidationStateUiModel.Done(
                    checkCredentialFreshness(it).toCredentialFreshnessSummaryModel()
                )
            )
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(60_000),
            initialValue = CredentialFreshnessValidationStateUiModel.Loading,
        )
    }
)

internal fun <Credential : Any, Match : Any> validatePresentationExchangeInputDescriptorMatches(
    inputDescriptorMatches: Map<String, Map<Credential, Match>>,
) {
    val missingDescriptorIds = inputDescriptorMatches
        .filterValues { it.isEmpty() }
        .keys
    check(missingDescriptorIds.isEmpty()) {
        "Presentation definition input descriptor(s) ${
            missingDescriptorIds.joinToString(", ")
        } did not match any stored credential"
    }
}
