package ui.composables.credentials

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.credentials.PowerOfRepresentationCredentialAdapter

@Composable
fun PowerOfRepresentationCredentialSummaryCardContent(
    credential: SubjectCredentialStore.StoreEntry,
) {
    val credentialAdapter = remember {
        PowerOfRepresentationCredentialAdapter.createFromStoreEntry(credential)
    }

    PowerOfRepresentationCredentialRepresentationDataCardContent(
        credentialAdapter = credentialAdapter,
    )
}