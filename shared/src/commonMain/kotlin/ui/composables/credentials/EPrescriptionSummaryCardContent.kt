package ui.composables.credentials

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.credentials.EPrescriptionCredentialAdapter

@Composable
fun EPrescriptionSummaryCardContent(
    credential: SubjectCredentialStore.StoreEntry,
) {
    val credentialAdapter = remember {
        EPrescriptionCredentialAdapter.createFromStoreEntry(credential)
    }

    EPrescriptionRepresentationDataCardContent(
        credentialAdapter = credentialAdapter,
    )
}