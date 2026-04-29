package ui.composables.credentials

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.credentials.AgeVerificationCredentialAdapter

@Composable
fun AgeVerificationCredentialSummaryCardContent(
    credential: SubjectCredentialStore.StoreEntry,
) {
    val credentialAdapter = remember {
        AgeVerificationCredentialAdapter.createFromStoreEntry(credential)
    }

    AgeVerificationCredentialAgeDataCardContentOverview(
        credentialAdapter = credentialAdapter,
    )
}
