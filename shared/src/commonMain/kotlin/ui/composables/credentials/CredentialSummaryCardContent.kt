package ui.composables.credentials

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.agent.SubjectCredentialStore

@Composable
fun CredentialSummaryCardContent(
    credential: SubjectCredentialStore.StoreEntry,
    imageDecoder: (ByteArray) -> ImageBitmap,
) {
    when (credential.scheme) {
        is IdAustriaScheme -> {
            IdAustriaCredentialSummaryCardContent(
                credential,
                imageDecoder = imageDecoder,
            )
        }

        else -> GenericCredentialSummaryCardContent(
            credential = credential,
        )
    }
}