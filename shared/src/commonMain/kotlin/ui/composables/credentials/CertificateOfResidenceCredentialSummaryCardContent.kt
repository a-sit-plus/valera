package ui.composables.credentials

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.credentials.CertificateOfResidenceCredentialAdapter

@Composable
fun CertificateOfResidenceCredentialSummaryCardContent(
    credential: SubjectCredentialStore.StoreEntry,
) {
    val credentialAdapter = remember {
        CertificateOfResidenceCredentialAdapter.createFromStoreEntry(credential)
    }

    CertificateOfResidenceCredentialIdentityDataCardContent(
        credentialAdapter = credentialAdapter,
    )
}