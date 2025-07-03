package ui.composables.credentials

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.credentials.IdAustriaCredentialAdapter

@Composable
fun IdAustriaCredentialSummaryCardContent(
    credential: SubjectCredentialStore.StoreEntry,
    decodeImage: (ByteArray) -> Result<ImageBitmap>,
) {
    val credentialAdapter = remember {
        IdAustriaCredentialAdapter.createFromStoreEntry(credential, decodeImage = decodeImage)
    }

    IdAustriaCredentialIdentityDataCardContent(
        credentialAdapter = credentialAdapter,
    )
}
