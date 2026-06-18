package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.ageverification.AgeVerificationScheme
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.isEuPid
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.isMdl
import at.asitplus.wallet.companyregistration.CompanyRegistrationScheme
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import at.asitplus.wallet.ehic.EhicScheme
import at.asitplus.wallet.healthid.HealthIdScheme
import at.asitplus.wallet.lib.data.CredentialScheme
import at.asitplus.wallet.por.PowerOfRepresentationScheme
import at.asitplus.wallet.taxid.TaxIdScheme
import org.jetbrains.compose.resources.StringResource

@Suppress("DEPRECATION")
interface CredentialAttributeTranslator {
    fun translateSingleClaimReference(claimReference: SingleClaimReference): StringResource?

    fun translate(attributeName: NormalizedJsonPath): StringResource?

    companion object {
        operator fun get(scheme: CredentialScheme?) = when {
            scheme.isEuPid -> EuPidCredentialAttributeTranslator()
            scheme.isMdl -> MobileDrivingLicenceCredentialAttributeTranslator()
            scheme is AgeVerificationScheme -> AgeVerificationCredentialAttributeTranslator()
            scheme is PowerOfRepresentationScheme -> PowerOfRepresentationCredentialAttributeTranslator()
            scheme is CertificateOfResidenceScheme -> CertificateOfResidenceCredentialAttributeTranslator()
            scheme is CompanyRegistrationScheme -> CompanyRegistrationCredentialAttributeTranslator()
            scheme is HealthIdScheme -> HealthIdCredentialAttributeTranslator()
            scheme is EhicScheme -> EhicCredentialAttributeTranslator()
            scheme is TaxIdScheme -> TaxIdCredentialAttributeTranslator()
            else -> null
        }
    }
}

