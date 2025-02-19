package ui.composables.credentials

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.taxid.TaxIdCredential
import data.credentials.EPrescriptionCredentialAdapter
import data.credentials.EuPidCredentialAdapter
import data.credentials.TaxIdCredentialAdapter

@Composable
fun TaxIdCredentialSummaryCardContent(
    credential: SubjectCredentialStore.StoreEntry,
    modifier: Modifier = Modifier,
) {
    val credentialAdapter = remember {
        TaxIdCredentialAdapter.createFromStoreEntry(credential)
    }

    TaxIdCredentialRepresentationDataCardContent(
        credentialAdapter = credentialAdapter,
    )
}