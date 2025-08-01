package at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data

import androidx.compose.runtime.Composable
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.valera.resources.*
import at.asitplus.wallet.companyregistration.CompanyRegistrationScheme
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import at.asitplus.wallet.ehic.EhicScheme
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.eupidsdjwt.EuPidSdJwtScheme
import at.asitplus.wallet.healthid.HealthIdScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import at.asitplus.wallet.por.PowerOfRepresentationScheme
import at.asitplus.wallet.taxid.TaxIdScheme
import data.credentials.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Suppress("DEPRECATION")
@Composable
fun ConstantIndex.CredentialScheme?.uiLabel(): String = when (this) {
    is IdAustriaScheme -> stringResource(Res.string.credential_scheme_label_id_austria)
    is EuPidScheme -> stringResource(Res.string.credential_scheme_label_eu_pid)
    is EuPidSdJwtScheme -> stringResource(Res.string.credential_scheme_label_eu_pid_sdjwt)
    is MobileDrivingLicenceScheme -> stringResource(Res.string.credential_scheme_label_mdl)
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
    is IdAustriaScheme -> getString(Res.string.credential_scheme_label_id_austria)
    is EuPidScheme -> getString(Res.string.credential_scheme_label_eu_pid)
    is EuPidSdJwtScheme -> getString(Res.string.credential_scheme_label_eu_pid_sdjwt)
    is MobileDrivingLicenceScheme -> getString(Res.string.credential_scheme_label_mdl)
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
    is IdAustriaScheme -> stringResource(Res.string.credential_scheme_icon_label_id_austria)
    is EuPidScheme -> stringResource(Res.string.credential_scheme_icon_label_eu_pid)
    is EuPidSdJwtScheme -> stringResource(Res.string.credential_scheme_icon_label_eu_pid)
    is MobileDrivingLicenceScheme -> stringResource(Res.string.credential_scheme_icon_label_mdl)
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
    is IdAustriaScheme -> { IdAustriaCredentialAttributeTranslator.translate(path) }
    is EuPidScheme -> { EuPidCredentialAttributeTranslator.translate(path) }
    is EuPidSdJwtScheme -> { EuPidCredentialAttributeTranslator.translate(path) }
    is MobileDrivingLicenceScheme -> { MobileDrivingLicenceCredentialAttributeTranslator.translate(path) }
    is PowerOfRepresentationScheme -> { PowerOfRepresentationCredentialAttributeTranslator.translate(path) }
    is CertificateOfResidenceScheme -> { CertificateOfResidenceCredentialAttributeTranslator.translate(path) }
    is CompanyRegistrationScheme -> { CompanyRegistrationCredentialAttributeTranslator.translate(path) }
    is HealthIdScheme -> { HealthIdCredentialAttributeTranslator.translate(path) }
    is EhicScheme -> { EhicCredentialAttributeTranslator.translate(path) }
    is TaxIdScheme -> { TaxIdCredentialAttributeTranslator.translate(path) }
    else -> { IdAustriaCredentialAttributeTranslator.translate(path) }
}
