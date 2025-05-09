package ui.composables.credentials

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.credentials.EhicCredentialAdapter

@Composable
fun EhicSummaryCardContent(
    credential: SubjectCredentialStore.StoreEntry,
) {
    val credentialAdapter = remember {
        EhicCredentialAdapter.createFromStoreEntry(credential)
    }

    EhicRepresentationDataCardContent(
        credentialAdapter = credentialAdapter,
    )
}