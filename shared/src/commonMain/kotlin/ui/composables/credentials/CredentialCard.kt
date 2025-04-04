package ui.composables.credentials

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.primitives.TokenStatus

@Composable
fun CredentialCard(
    credential: SubjectCredentialStore.StoreEntry,
    tokenStatus: TokenStatus?,
    imageDecoder: (ByteArray) -> ImageBitmap?,
    onDelete: () -> Unit,
    onOpenDetails: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    CredentialCardLayout(
        modifier = modifier,
    ) {
        CredentialCardHeader(
            credential = credential,
            tokenStatus = tokenStatus,
            onDelete = onDelete,
            modifier = Modifier.fillMaxWidth(),
        )
        CredentialSummaryCardContent(
            credential = credential,
            decodeToBitmap = imageDecoder,
        )
        CredentialCardFooter(onOpenDetails, modifier = Modifier.fillMaxWidth())
    }
}
