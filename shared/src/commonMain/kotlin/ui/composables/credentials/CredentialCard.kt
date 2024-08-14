package ui.composables.credentials

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.agent.SubjectCredentialStore

@Composable
fun CredentialCard(
    credential: SubjectCredentialStore.StoreEntry,
    imageDecoder: (ByteArray) -> ImageBitmap,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    CredentialCardLayout(
        modifier = modifier,
    ) {
        CredentialCardHeader(
            credential = credential,
            onDelete = onDelete,
            modifier = Modifier.fillMaxWidth(),
        )
        when (credential.scheme) {
            is IdAustriaScheme -> {
                IdAustriaCredentialSummaryCardContent(
                    credential,
                    imageDecoder = imageDecoder,
                )
            }

            else -> GenericCredentialSummaryCardContent(
                credential = credential,
                onDelete = onDelete,
            )
        }
    }
}