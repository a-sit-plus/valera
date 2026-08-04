package ui.composables.credentials

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import data.credentials.MobileDrivingLicenceCredentialAdapter
import ui.models.ResolvedCredential

@Composable
fun MobileDrivingLicenceCredentialSummaryCardContent(
    credential: ResolvedCredential,
    decodeToBitmap: (ByteArray) -> Result<ImageBitmap>,
    modifier: Modifier = Modifier,
) {
    val credentialAdapter = remember(credential) {
        MobileDrivingLicenceCredentialAdapter.createFromStoreEntry(credential.entry, credential.scheme, decodeToBitmap)
    }

    MobileDrivingLicenceCredentialIdentityDataCardContent(
        credentialAdapter = credentialAdapter,
    )
}
