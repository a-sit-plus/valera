package ui.composables.credentials

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import data.credentials.EuPidCredentialAdapter
import ui.models.ResolvedCredential

@Composable
fun EuPidCredentialSummaryCardContent(
    credential: ResolvedCredential,
    decodeToBitmap: (ByteArray) -> Result<ImageBitmap>,
    modifier: Modifier = Modifier,
) {
    val credentialAdapter = remember(credential) {
        EuPidCredentialAdapter.createFromStoreEntry(credential.entry, credential.scheme, decodeToBitmap)
    }

    EuPidCredentialIdentityDataCardContent(
        credentialAdapter = credentialAdapter,
        modifier = modifier,
    )
}
