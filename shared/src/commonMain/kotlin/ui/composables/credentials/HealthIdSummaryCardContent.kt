package ui.composables.credentials

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.credentials.HealthIdCredentialAdapter

@Composable
fun HealthIdSummaryCardContent(
    credential: SubjectCredentialStore.StoreEntry,
) {
    val credentialAdapter = remember {
        HealthIdCredentialAdapter.createFromStoreEntry(credential)
    }

    HealthIdRepresentationDataCardContent(
        credentialAdapter = credentialAdapter,
    )
}