package ui.composables.credentials

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.credentials.EuPidCredentialAdapter

@Composable
fun EuPidCredentialSummaryCardContent(
    credential: SubjectCredentialStore.StoreEntry,
    decodeToBitmap: (ByteArray) -> Result<ImageBitmap>,
    modifier: Modifier = Modifier,
) {
    val credentialAdapter = remember {
        EuPidCredentialAdapter.createFromStoreEntry(credential, decodeToBitmap)
    }

    EuPidCredentialIdentityDataCardContent(
        credentialAdapter = credentialAdapter,
        modifier = modifier,
    )
}