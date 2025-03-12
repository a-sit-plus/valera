package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.companyregistration.CompanyRegistrationScheme
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.healthid.HealthIdScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import at.asitplus.wallet.por.PowerOfRepresentationScheme
import at.asitplus.wallet.taxid.TaxIdScheme
import org.jetbrains.compose.resources.StringResource

interface CredentialAttributeTranslator {
    fun translate(attributeName: NormalizedJsonPath): StringResource?

    companion object {
        operator fun get(scheme: ConstantIndex.CredentialScheme?) = when(scheme) {
            is IdAustriaScheme -> IdAustriaCredentialAttributeTranslator
            is EuPidScheme -> EuPidCredentialAttributeTranslator
            is MobileDrivingLicenceScheme -> MobileDrivingLicenceCredentialAttributeTranslator
            is PowerOfRepresentationScheme -> PowerOfRepresentationCredentialAttributeTranslator
            is CertificateOfResidenceScheme -> CertificateOfResidenceCredentialAttributeTranslator
            is CompanyRegistrationScheme -> CompanyRegistrationCredentialAttributeTranslator
            is HealthIdScheme -> HealthIdCredentialAttributeTranslator
            is TaxIdScheme -> TaxIdCredentialAttributeTranslator
            else -> null
        }
    }
}

