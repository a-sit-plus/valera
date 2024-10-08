package ui.composables.credentials

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.credentials.CertificateOfResidenceCredentialAdapter
import data.credentials.PowerOfRepresentationCredentialAdapter

@Composable
fun CertificateOfResidenceCredentialView(
    credential: SubjectCredentialStore.StoreEntry,
    modifier: Modifier = Modifier,
) {
    val credentialAdapter = remember {
        CertificateOfResidenceCredentialAdapter.createFromStoreEntry(
            credential,
        )
    }

    Column(modifier = modifier) {
        val spacingModifier = Modifier.padding(bottom = 16.dp)
        CertificateOfResidenceCredentialIdentityDataCard(
            credentialAdapter = credentialAdapter,
            modifier = spacingModifier,
        )
        CertificateOfResidenceCredentialMetaDataCard(
            credentialAdapter = credentialAdapter,
            modifier = spacingModifier,
        )
    }
}
