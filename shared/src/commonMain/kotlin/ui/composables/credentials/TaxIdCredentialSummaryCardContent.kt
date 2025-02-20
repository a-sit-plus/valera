package ui.composables.credentials

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.credentials.TaxIdCredentialAdapter

@Composable
fun TaxIdCredentialSummaryCardContent(
    credential: SubjectCredentialStore.StoreEntry,
    modifier: Modifier = Modifier,
) {
    val credentialAdapter = remember {
        TaxIdCredentialAdapter.createFromStoreEntry(credential)
    }

    TaxIdCredentialMetadataDataCardContent(
        credentialAdapter = credentialAdapter,
    )
}