package ui.composables.credentials

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.wallet.ageverification.AgeVerificationScheme
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.isEuPid
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.isMdl
import at.asitplus.wallet.companyregistration.CompanyRegistrationScheme
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import at.asitplus.wallet.ehic.EhicScheme
import at.asitplus.wallet.healthid.HealthIdScheme
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.por.PowerOfRepresentationScheme
import at.asitplus.wallet.taxid.TaxIdScheme

@Composable
fun CredentialSummaryCardContent(
    credential: SubjectCredentialStore.StoreEntry,
    decodeToBitmap: (ByteArray) -> Result<ImageBitmap>,
) {
    @Suppress("DEPRECATION")
    credential.scheme.let { s ->
        when {
            s.isEuPid -> EuPidCredentialSummaryCardContent(credential, decodeToBitmap)
            s.isMdl -> MobileDrivingLicenceCredentialSummaryCardContent(credential, decodeToBitmap)
            s is AgeVerificationScheme -> AgeVerificationCredentialSummaryCardContent(credential)
            s is PowerOfRepresentationScheme -> PowerOfRepresentationCredentialSummaryCardContent(credential)
            s is CertificateOfResidenceScheme -> CertificateOfResidenceCredentialSummaryCardContent(credential)
            s is CompanyRegistrationScheme -> CompanyRegistrationCredentialSummaryCardContent(credential)
            s is HealthIdScheme -> HealthIdSummaryCardContent(credential)
            s is EhicScheme -> EhicSummaryCardContent(credential)
            s is TaxIdScheme -> TaxIdCredentialSummaryCardContent(credential)
            else -> {}
        }
    }
}
