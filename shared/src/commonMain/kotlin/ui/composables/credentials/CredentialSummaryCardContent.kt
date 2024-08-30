package ui.composables.credentials

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.wallet.cor.CertificateOfResidence
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import at.asitplus.wallet.por.PowerOfRepresentationScheme

@Composable
fun CredentialSummaryCardContent(
    credential: SubjectCredentialStore.StoreEntry,
    decodeToBitmap: (ByteArray) -> ImageBitmap?,
) {
    when (credential.scheme) {
        is IdAustriaScheme -> IdAustriaCredentialSummaryCardContent(
            credential,
            decodeImage = decodeToBitmap,
        )


        is EuPidScheme -> EuPidCredentialSummaryCardContent(credential)

        is MobileDrivingLicenceScheme -> MobileDrivingLicenceCredentialSummaryCardContent(
            credential,
            decodeToBitmap = decodeToBitmap,
        )

        is PowerOfRepresentationScheme -> PowerOfRepresentationCredentialSummaryCardContent(
            credential,
        )

        is CertificateOfResidenceScheme -> CertificateOfResidenceCredentialSummaryCardContent(
            credential,
        )

        else -> GenericCredentialSummaryCardContent(
            credential = credential,
        )
    }
}