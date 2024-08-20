package ui.composables.credentials

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme

@Composable
fun CredentialSummaryCardContent(
    credential: SubjectCredentialStore.StoreEntry,
    decodeToBitmap: (ByteArray) -> ImageBitmap,
) {
    when (credential.scheme) {
        is IdAustriaScheme -> {
            IdAustriaCredentialSummaryCardContent(
                credential,
                decodeImage = decodeToBitmap,
            )
        }

        is EuPidScheme -> EuPidCredentialSummaryCardContent(credential)

        is MobileDrivingLicenceScheme -> MobileDrivingLicenceCredentialSummaryCardContent(
            credential,
            decodeToBitmap = decodeToBitmap,
        )

        else -> GenericCredentialSummaryCardContent(
            credential = credential,
        )
    }
}