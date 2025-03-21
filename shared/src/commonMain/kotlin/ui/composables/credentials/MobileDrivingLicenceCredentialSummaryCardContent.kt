package ui.composables.credentials

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.credentials.MobileDrivingLicenceCredentialAdapter

@Composable
fun MobileDrivingLicenceCredentialSummaryCardContent(
    credential: SubjectCredentialStore.StoreEntry,
    decodeToBitmap: (ByteArray) -> ImageBitmap?,
    modifier: Modifier = Modifier,
) {
    val credentialAdapter = remember {
        MobileDrivingLicenceCredentialAdapter.createFromStoreEntry(credential, decodeToBitmap)
    }

    MobileDrivingLicenceCredentialIdentityDataCardContent(
        credentialAdapter = credentialAdapter,
    )
}
