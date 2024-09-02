package ui.composables.credentials

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import at.asitplus.wallet.cor.CertificateOfResidence
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.credentials.CertificateOfResidenceCredentialAdapter
import data.credentials.PowerOfRepresentationCredentialAdapter

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