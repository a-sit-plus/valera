package ui.composables.credentials

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.wallet.companyregistration.CompanyRegistrationScheme
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import at.asitplus.wallet.ehic.EhicScheme
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.eupidsdjwt.EuPidSdJwtScheme
import at.asitplus.wallet.healthid.HealthIdScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import at.asitplus.wallet.por.PowerOfRepresentationScheme
import at.asitplus.wallet.taxid.TaxIdScheme

@Composable
fun CredentialSummaryCardContent(
    credential: SubjectCredentialStore.StoreEntry,
    decodeToBitmap: (ByteArray) -> Result<ImageBitmap>,
) {
    @Suppress("DEPRECATION")
    when (credential.scheme) {
        is IdAustriaScheme -> IdAustriaCredentialSummaryCardContent(credential, decodeToBitmap)
        is EuPidScheme -> EuPidCredentialSummaryCardContent(credential, decodeToBitmap)
        is EuPidSdJwtScheme -> EuPidCredentialSummaryCardContent(credential, decodeToBitmap)
        is MobileDrivingLicenceScheme -> MobileDrivingLicenceCredentialSummaryCardContent(credential, decodeToBitmap)
        is PowerOfRepresentationScheme -> PowerOfRepresentationCredentialSummaryCardContent(credential)
        is CertificateOfResidenceScheme -> CertificateOfResidenceCredentialSummaryCardContent(credential)
        is CompanyRegistrationScheme -> CompanyRegistrationCredentialSummaryCardContent(credential)
        is HealthIdScheme -> HealthIdSummaryCardContent(credential)
        is EhicScheme -> EhicSummaryCardContent(credential)
        is TaxIdScheme -> TaxIdCredentialSummaryCardContent(credential)
        else -> {}
    }
}
