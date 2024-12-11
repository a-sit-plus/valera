package ui.composables.credentials

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.wallet.lib.agent.SubjectCredentialStore

@Composable
fun CredentialSelectionCard(
    credential: SubjectCredentialStore.StoreEntry,
    imageDecoder: (ByteArray) -> ImageBitmap?,
    selectedCredential: MutableState<SubjectCredentialStore.StoreEntry>,
    modifier: Modifier = Modifier,
) {
    CredentialCardLayout(
        modifier = modifier,
    ) {
        CredentialSelectionCardHeader(
            credential = credential,
            modifier = Modifier.fillMaxWidth(),
        )
        CredentialSummaryCardContent(
            credential = credential,
            decodeToBitmap = imageDecoder,
        )
        CredentialSelectionCardFooter(credential, selectedCredential, modifier = Modifier.fillMaxWidth())
    }
}
