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
import at.asitplus.wallet.healthid.HealthIdScheme
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.CredentialScheme
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
fun CredentialScheme?.uiLabel(): String = when {
    isEuPidSdJwt -> stringResource(Res.string.credential_scheme_label_eu_pid_sdjwt)
    isEuPid -> stringResource(Res.string.credential_scheme_label_eu_pid)
    isMdl -> stringResource(Res.string.credential_scheme_label_mdl)
    this is AgeVerificationScheme -> stringResource(Res.string.credential_scheme_label_av)
    this is PowerOfRepresentationScheme -> stringResource(Res.string.credential_scheme_label_power_of_representation)
    this is CertificateOfResidenceScheme -> stringResource(Res.string.credential_scheme_label_certificate_of_residence)
    this is CompanyRegistrationScheme -> stringResource(Res.string.credential_scheme_label_company_registration)
    this is HealthIdScheme -> stringResource(Res.string.credential_scheme_label_healthid)
    this is EhicScheme -> stringResource(Res.string.credential_scheme_label_ehic)
    this is TaxIdScheme -> stringResource(Res.string.credential_scheme_label_tax_id_2025)
    else -> this?.identifier ?: "unknown"
}

@Suppress("DEPRECATION")
suspend fun CredentialScheme?.uiLabelNonCompose(): String = when {
    isEuPidSdJwt -> getString(Res.string.credential_scheme_label_eu_pid_sdjwt)
    isEuPid -> getString(Res.string.credential_scheme_label_eu_pid)
    isMdl -> getString(Res.string.credential_scheme_label_mdl)
    this is AgeVerificationScheme -> getString(Res.string.credential_scheme_label_av)
    this is PowerOfRepresentationScheme -> getString(Res.string.credential_scheme_label_power_of_representation)
    this is CertificateOfResidenceScheme -> getString(Res.string.credential_scheme_label_certificate_of_residence)
    this is CompanyRegistrationScheme -> getString(Res.string.credential_scheme_label_company_registration)
    this is HealthIdScheme -> getString(Res.string.credential_scheme_label_healthid)
    this is EhicScheme -> getString(Res.string.credential_scheme_label_ehic)
    this is TaxIdScheme -> getString(Res.string.credential_scheme_label_tax_id_2025)
    else -> this?.identifier ?: "unknown"
}

@Suppress("DEPRECATION")
@Composable
fun CredentialScheme?.iconLabel(): String = when {
    isEuPid -> stringResource(Res.string.credential_scheme_icon_label_eu_pid)
    isMdl -> stringResource(Res.string.credential_scheme_icon_label_mdl)
    this is AgeVerificationScheme -> stringResource(Res.string.credential_scheme_icon_label_av)
    this is PowerOfRepresentationScheme -> stringResource(Res.string.credential_scheme_icon_label_power_of_representation)
    this is CertificateOfResidenceScheme -> stringResource(Res.string.credential_scheme_icon_label_certificate_of_residence)
    this is CompanyRegistrationScheme -> stringResource(Res.string.credential_scheme_icon_label_company_registration)
    this is HealthIdScheme -> stringResource(Res.string.credential_scheme_icon_label_healthid)
    this is EhicScheme -> stringResource(Res.string.credential_scheme_icon_label_ehic)
    this is TaxIdScheme -> stringResource(Res.string.credential_scheme_icon_label_tax_id)
    else -> this?.identifier ?: "unknown"
}

@Suppress("DEPRECATION")
fun CredentialScheme.getLocalization(path: NormalizedJsonPath): StringResource? = when {
    isEuPid -> { EuPidCredentialAttributeTranslator().translate(path) }
    isMdl -> { MobileDrivingLicenceCredentialAttributeTranslator().translate(path) }
    this is AgeVerificationScheme -> { AgeVerificationCredentialAttributeTranslator().translate(path) }
    this is PowerOfRepresentationScheme -> { PowerOfRepresentationCredentialAttributeTranslator().translate(path) }
    this is CertificateOfResidenceScheme -> { CertificateOfResidenceCredentialAttributeTranslator().translate(path) }
    this is CompanyRegistrationScheme -> { CompanyRegistrationCredentialAttributeTranslator().translate(path) }
    this is HealthIdScheme -> { HealthIdCredentialAttributeTranslator().translate(path) }
    this is EhicScheme -> { EhicCredentialAttributeTranslator().translate(path) }
    this is TaxIdScheme -> TaxIdCredentialAttributeTranslator().translate(path)
    else -> { EuPidCredentialAttributeTranslator().translate(path) }
}


@Suppress("DEPRECATION")
fun CredentialScheme.getLocalization(claimReference: SingleClaimReference): StringResource? = when {
    isEuPid -> { EuPidCredentialAttributeTranslator().translateSingleClaimReference(claimReference) }
    isMdl -> { MobileDrivingLicenceCredentialAttributeTranslator().translateSingleClaimReference(claimReference) }
    this is AgeVerificationScheme -> { AgeVerificationCredentialAttributeTranslator().translateSingleClaimReference(claimReference) }
    this is PowerOfRepresentationScheme -> { PowerOfRepresentationCredentialAttributeTranslator().translateSingleClaimReference(claimReference) }
    this is CertificateOfResidenceScheme -> { CertificateOfResidenceCredentialAttributeTranslator().translateSingleClaimReference(claimReference) }
    this is CompanyRegistrationScheme -> { CompanyRegistrationCredentialAttributeTranslator().translateSingleClaimReference(claimReference) }
    this is HealthIdScheme -> { HealthIdCredentialAttributeTranslator().translateSingleClaimReference(claimReference) }
    this is EhicScheme -> { EhicCredentialAttributeTranslator().translateSingleClaimReference(claimReference) }
    this is TaxIdScheme -> TaxIdCredentialAttributeTranslator().translateSingleClaimReference(claimReference)
    else -> { EuPidCredentialAttributeTranslator().translateSingleClaimReference(claimReference) }
}
