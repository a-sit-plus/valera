package at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data

import androidx.compose.runtime.Composable
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.credential_scheme_icon_label_av
import at.asitplus.valera.resources.credential_scheme_icon_label_certificate_of_residence
import at.asitplus.valera.resources.credential_scheme_icon_label_company_registration
import at.asitplus.valera.resources.credential_scheme_icon_label_ehic
import at.asitplus.valera.resources.credential_scheme_icon_label_eu_pid
import at.asitplus.valera.resources.credential_scheme_icon_label_healthid
import at.asitplus.valera.resources.credential_scheme_icon_label_mdl
import at.asitplus.valera.resources.credential_scheme_icon_label_power_of_representation
import at.asitplus.valera.resources.credential_scheme_icon_label_tax_id
import at.asitplus.valera.resources.credential_scheme_label_av
import at.asitplus.valera.resources.credential_scheme_label_certificate_of_residence
import at.asitplus.valera.resources.credential_scheme_label_company_registration
import at.asitplus.valera.resources.credential_scheme_label_ehic
import at.asitplus.valera.resources.credential_scheme_label_eu_pid
import at.asitplus.valera.resources.credential_scheme_label_eu_pid_sdjwt
import at.asitplus.valera.resources.credential_scheme_label_healthid
import at.asitplus.valera.resources.credential_scheme_label_mdl
import at.asitplus.valera.resources.credential_scheme_label_power_of_representation
import at.asitplus.valera.resources.credential_scheme_label_tax_id_2025
import at.asitplus.wallet.ageverification.AgeVerificationScheme
import at.asitplus.wallet.companyregistration.CompanyRegistrationScheme
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import at.asitplus.wallet.ehic.EhicScheme
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.eupidsdjwt.EuPidSdJwtScheme
import at.asitplus.wallet.healthid.HealthIdScheme
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import at.asitplus.wallet.por.PowerOfRepresentationScheme
import at.asitplus.wallet.taxid.TaxIdScheme
import data.credentials.AgeVerificationCredentialAttributeTranslator
import data.credentials.CertificateOfResidenceCredentialAttributeTranslator
import data.credentials.CompanyRegistrationCredentialAttributeTranslator
import data.credentials.EhicCredentialAttributeTranslator
import data.credentials.EuPidCredentialAttributeTranslator
import data.credentials.HealthIdCredentialAttributeTranslator
import data.credentials.MobileDrivingLicenceCredentialAttributeTranslator
import data.credentials.PowerOfRepresentationCredentialAttributeTranslator
import data.credentials.SingleClaimReference
import data.credentials.TaxIdCredentialAttributeTranslator
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Suppress("DEPRECATION")
@Composable
fun ConstantIndex.CredentialScheme?.uiLabel(): String = when (this) {
    is EuPidScheme -> stringResource(Res.string.credential_scheme_label_eu_pid)
    is EuPidSdJwtScheme -> stringResource(Res.string.credential_scheme_label_eu_pid_sdjwt)
    is MobileDrivingLicenceScheme -> stringResource(Res.string.credential_scheme_label_mdl)
    is AgeVerificationScheme -> stringResource(Res.string.credential_scheme_label_av)
    is PowerOfRepresentationScheme -> stringResource(Res.string.credential_scheme_label_power_of_representation)
    is CertificateOfResidenceScheme -> stringResource(Res.string.credential_scheme_label_certificate_of_residence)
    is CompanyRegistrationScheme -> stringResource(Res.string.credential_scheme_label_company_registration)
    is HealthIdScheme -> stringResource(Res.string.credential_scheme_label_healthid)
    is EhicScheme -> stringResource(Res.string.credential_scheme_label_ehic)
    is TaxIdScheme -> stringResource(Res.string.credential_scheme_label_tax_id_2025)
    else -> this?.identifier ?: "unknown"
}

@Suppress("DEPRECATION")
suspend fun ConstantIndex.CredentialScheme?.uiLabelNonCompose(): String = when (this) {
    is EuPidScheme -> getString(Res.string.credential_scheme_label_eu_pid)
    is EuPidSdJwtScheme -> getString(Res.string.credential_scheme_label_eu_pid_sdjwt)
    is MobileDrivingLicenceScheme -> getString(Res.string.credential_scheme_label_mdl)
    is AgeVerificationScheme -> getString(Res.string.credential_scheme_label_av)
    is PowerOfRepresentationScheme -> getString(Res.string.credential_scheme_label_power_of_representation)
    is CertificateOfResidenceScheme -> getString(Res.string.credential_scheme_label_certificate_of_residence)
    is CompanyRegistrationScheme -> getString(Res.string.credential_scheme_label_company_registration)
    is HealthIdScheme -> getString(Res.string.credential_scheme_label_healthid)
    is EhicScheme -> getString(Res.string.credential_scheme_label_ehic)
    is TaxIdScheme -> getString(Res.string.credential_scheme_label_tax_id_2025)
    else -> this?.identifier ?: "unknown"
}

@Suppress("DEPRECATION")
@Composable
fun ConstantIndex.CredentialScheme?.iconLabel(): String = when (this) {
    is EuPidScheme -> stringResource(Res.string.credential_scheme_icon_label_eu_pid)
    is EuPidSdJwtScheme -> stringResource(Res.string.credential_scheme_icon_label_eu_pid)
    is MobileDrivingLicenceScheme -> stringResource(Res.string.credential_scheme_icon_label_mdl)
    is AgeVerificationScheme -> stringResource(Res.string.credential_scheme_icon_label_av)
    is PowerOfRepresentationScheme -> stringResource(Res.string.credential_scheme_icon_label_power_of_representation)
    is CertificateOfResidenceScheme -> stringResource(Res.string.credential_scheme_icon_label_certificate_of_residence)
    is CompanyRegistrationScheme -> stringResource(Res.string.credential_scheme_icon_label_company_registration)
    is HealthIdScheme -> stringResource(Res.string.credential_scheme_icon_label_healthid)
    is EhicScheme -> stringResource(Res.string.credential_scheme_icon_label_ehic)
    is TaxIdScheme -> stringResource(Res.string.credential_scheme_icon_label_tax_id)
    else -> this?.identifier ?: "unknown"
}

@Suppress("DEPRECATION")
fun ConstantIndex.CredentialScheme.getLocalization(path: NormalizedJsonPath): StringResource? = when (this) {
    is EuPidScheme -> { EuPidCredentialAttributeTranslator().translate(path) }
    is EuPidSdJwtScheme -> { EuPidCredentialAttributeTranslator().translate(path) }
    is MobileDrivingLicenceScheme -> { MobileDrivingLicenceCredentialAttributeTranslator().translate(path) }
    is AgeVerificationScheme -> { AgeVerificationCredentialAttributeTranslator().translate(path) }
    is PowerOfRepresentationScheme -> { PowerOfRepresentationCredentialAttributeTranslator().translate(path) }
    is CertificateOfResidenceScheme -> { CertificateOfResidenceCredentialAttributeTranslator().translate(path) }
    is CompanyRegistrationScheme -> { CompanyRegistrationCredentialAttributeTranslator().translate(path) }
    is HealthIdScheme -> { HealthIdCredentialAttributeTranslator().translate(path) }
    is EhicScheme -> { EhicCredentialAttributeTranslator().translate(path) }
    is TaxIdScheme -> TaxIdCredentialAttributeTranslator().translate(path)
    else -> { EuPidCredentialAttributeTranslator().translate(path) }
}


@Suppress("DEPRECATION")
fun ConstantIndex.CredentialScheme.getLocalization(claimReference: SingleClaimReference): StringResource? = when (this) {
    is EuPidScheme -> { EuPidCredentialAttributeTranslator().translateSingleClaimReference(claimReference) }
    is EuPidSdJwtScheme -> { EuPidCredentialAttributeTranslator().translateSingleClaimReference(claimReference) }
    is MobileDrivingLicenceScheme -> { MobileDrivingLicenceCredentialAttributeTranslator().translateSingleClaimReference(claimReference) }
    is AgeVerificationScheme -> { AgeVerificationCredentialAttributeTranslator().translateSingleClaimReference(claimReference) }
    is PowerOfRepresentationScheme -> { PowerOfRepresentationCredentialAttributeTranslator().translateSingleClaimReference(claimReference) }
    is CertificateOfResidenceScheme -> { CertificateOfResidenceCredentialAttributeTranslator().translateSingleClaimReference(claimReference) }
    is CompanyRegistrationScheme -> { CompanyRegistrationCredentialAttributeTranslator().translateSingleClaimReference(claimReference) }
    is HealthIdScheme -> { HealthIdCredentialAttributeTranslator().translateSingleClaimReference(claimReference) }
    is EhicScheme -> { EhicCredentialAttributeTranslator().translateSingleClaimReference(claimReference) }
    is TaxIdScheme -> TaxIdCredentialAttributeTranslator().translateSingleClaimReference(claimReference)
    else -> { EuPidCredentialAttributeTranslator().translateSingleClaimReference(claimReference) }
}
