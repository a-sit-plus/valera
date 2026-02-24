package ui.models

import at.asitplus.wallet.lib.agent.SubjectCredentialStore

fun interface CredentialFreshnessSummaryModelEvaluator {
    suspend operator fun invoke(
        storeEntry: SubjectCredentialStore.StoreEntry,
    ): CredentialFreshnessSummaryUiModel
}