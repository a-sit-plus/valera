package ui.composables.credentials

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import data.credentials.EuPidCredentialAdapter
import ui.models.ResolvedCredential

@Composable
fun EuPidCredentialView(
    credential: ResolvedCredential,
    decodeImage: (ByteArray) -> Result<ImageBitmap>,
    modifier: Modifier = Modifier,
) {
    val credentialAdapter = remember(credential) {
        EuPidCredentialAdapter.createFromStoreEntry(credential.entry, credential.scheme, decodeImage)
    }
    EuPidCredentialViewFromAdapter(credentialAdapter, modifier)
}

@Composable
fun EuPidCredentialViewFromAdapter(
    credentialAdapter: EuPidCredentialAdapter,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        val spacingModifier = Modifier.padding(bottom = 16.dp)
        EuPidCredentialIdentityDataCard(credentialAdapter, spacingModifier)
        EuPidCredentialBirthdataDataCard(credentialAdapter, spacingModifier)
        EuPidCredentialResidenceDataCard(credentialAdapter, spacingModifier)
        EuPidCredentialMetadataCard(credentialAdapter, spacingModifier)
    }
}
