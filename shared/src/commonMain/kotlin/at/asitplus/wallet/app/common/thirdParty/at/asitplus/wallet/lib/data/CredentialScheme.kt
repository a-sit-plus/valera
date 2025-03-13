package at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data

import androidx.compose.runtime.Composable
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.credential_scheme_icon_label_certificate_of_residence
import at.asitplus.valera.resources.credential_scheme_icon_label_company_registration
import at.asitplus.valera.resources.credential_scheme_icon_label_eu_pid
import at.asitplus.valera.resources.credential_scheme_icon_label_healthid
import at.asitplus.valera.resources.credential_scheme_icon_label_id_austria
import at.asitplus.valera.resources.credential_scheme_icon_label_mdl
import at.asitplus.valera.resources.credential_scheme_icon_label_power_of_representation
import at.asitplus.valera.resources.credential_scheme_icon_label_tax_id
import at.asitplus.valera.resources.credential_scheme_label_certificate_of_residence
import at.asitplus.valera.resources.credential_scheme_label_company_registration
import at.asitplus.valera.resources.credential_scheme_label_eu_pid
import at.asitplus.valera.resources.credential_scheme_label_healthid
import at.asitplus.valera.resources.credential_scheme_label_id_austria
import at.asitplus.valera.resources.credential_scheme_label_mdl
import at.asitplus.valera.resources.credential_scheme_label_power_of_representation
import at.asitplus.valera.resources.credential_scheme_label_tax_id
import at.asitplus.wallet.companyregistration.CompanyRegistrationScheme
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.healthid.HealthIdScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import at.asitplus.wallet.por.PowerOfRepresentationScheme
import at.asitplus.wallet.taxid.TaxIdScheme
import data.credentials.CertificateOfResidenceCredentialAttributeTranslator
import data.credentials.CompanyRegistrationCredentialAttributeTranslator
import data.credentials.EuPidCredentialAttributeTranslator
import data.credentials.HealthIdCredentialAttributeTranslator
import data.credentials.IdAustriaCredentialAttributeTranslator
import data.credentials.MobileDrivingLicenceCredentialAttributeTranslator
import data.credentials.PowerOfRepresentationCredentialAttributeTranslator
import data.credentials.TaxIdCredentialAttributeTranslator
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Composable
fun ConstantIndex.CredentialScheme?.uiLabel(): String = when (this) {
    is IdAustriaScheme -> stringResource(Res.string.credential_scheme_label_id_austria)
    is EuPidScheme -> stringResource(Res.string.credential_scheme_label_eu_pid)
    is MobileDrivingLicenceScheme -> stringResource(Res.string.credential_scheme_label_mdl)
    is PowerOfRepresentationScheme -> stringResource(Res.string.credential_scheme_label_power_of_representation)
    is CertificateOfResidenceScheme -> stringResource(Res.string.credential_scheme_label_certificate_of_residence)
    is CompanyRegistrationScheme -> stringResource(Res.string.credential_scheme_label_company_registration)
    is HealthIdScheme -> stringResource(Res.string.credential_scheme_label_healthid)
    is TaxIdScheme -> stringResource(Res.string.credential_scheme_label_tax_id)
    else -> this?.identifier ?: "unknown"
}

suspend fun ConstantIndex.CredentialScheme?.uiLabelNonCompose(): String = when (this) {
    is IdAustriaScheme -> getString(Res.string.credential_scheme_label_id_austria)
    is EuPidScheme -> getString(Res.string.credential_scheme_label_eu_pid)
    is MobileDrivingLicenceScheme -> getString(Res.string.credential_scheme_label_mdl)
    is PowerOfRepresentationScheme -> getString(Res.string.credential_scheme_label_power_of_representation)
    is CertificateOfResidenceScheme -> getString(Res.string.credential_scheme_label_certificate_of_residence)
    is CompanyRegistrationScheme -> getString(Res.string.credential_scheme_label_company_registration)
    is HealthIdScheme -> getString(Res.string.credential_scheme_label_healthid)
    is TaxIdScheme -> getString(Res.string.credential_scheme_label_tax_id)
    else -> this?.identifier ?: "unknown"
}

@Composable
fun ConstantIndex.CredentialScheme?.iconLabel(): String = when (this) {
    is IdAustriaScheme -> stringResource(Res.string.credential_scheme_icon_label_id_austria)
    is EuPidScheme -> stringResource(Res.string.credential_scheme_icon_label_eu_pid)
    is MobileDrivingLicenceScheme -> stringResource(Res.string.credential_scheme_icon_label_mdl)
    is PowerOfRepresentationScheme -> stringResource(Res.string.credential_scheme_icon_label_power_of_representation)
    is CertificateOfResidenceScheme -> stringResource(Res.string.credential_scheme_icon_label_certificate_of_residence)
    is CompanyRegistrationScheme -> stringResource(Res.string.credential_scheme_icon_label_company_registration)
    is HealthIdScheme -> stringResource(Res.string.credential_scheme_icon_label_healthid)
    is TaxIdScheme -> stringResource(Res.string.credential_scheme_icon_label_tax_id)
    else -> this?.identifier ?: "unknown"
}

fun ConstantIndex.CredentialScheme.getLocalization(path: NormalizedJsonPath): StringResource? = when (this) {
    is MobileDrivingLicenceScheme -> { MobileDrivingLicenceCredentialAttributeTranslator.translate(path) }
    is CertificateOfResidenceScheme -> { CertificateOfResidenceCredentialAttributeTranslator.translate(path) }
    is PowerOfRepresentationScheme -> { PowerOfRepresentationCredentialAttributeTranslator.translate(path) }
    is CompanyRegistrationScheme -> { CompanyRegistrationCredentialAttributeTranslator.translate(path) }
    is HealthIdScheme -> { HealthIdCredentialAttributeTranslator.translate(path) }
    is EuPidScheme -> { EuPidCredentialAttributeTranslator.translate(path) }
    is IdAustriaScheme -> { IdAustriaCredentialAttributeTranslator.translate(path) }
    is TaxIdScheme -> { TaxIdCredentialAttributeTranslator.translate(path) }
    else -> { IdAustriaCredentialAttributeTranslator.translate(path) }
}
