package at.asitplus.wallet.app.common.third_party.at.asitplus.wallet.lib.data

import androidx.compose.runtime.Composable
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.credential_scheme_icon_label_certificate_of_residence
import at.asitplus.valera.resources.credential_scheme_icon_label_eprescription
import at.asitplus.valera.resources.credential_scheme_icon_label_eu_pid
import at.asitplus.valera.resources.credential_scheme_icon_label_id_austria
import at.asitplus.valera.resources.credential_scheme_icon_label_mdl
import at.asitplus.valera.resources.credential_scheme_icon_label_power_of_representation
import at.asitplus.valera.resources.credential_scheme_label_certificate_of_residence
import at.asitplus.valera.resources.credential_scheme_label_eprescription
import at.asitplus.valera.resources.credential_scheme_label_eu_pid
import at.asitplus.valera.resources.credential_scheme_label_id_austria
import at.asitplus.valera.resources.credential_scheme_label_mdl
import at.asitplus.valera.resources.credential_scheme_label_power_of_representation
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import at.asitplus.wallet.eprescription.EPrescriptionScheme
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import at.asitplus.wallet.por.PowerOfRepresentationScheme
import data.credentials.CertificateOfResidenceCredentialAttributeTranslator
import data.credentials.EPrescriptionCredentialAttributeTranslator
import data.credentials.EuPidCredentialAttributeTranslator
import data.credentials.IdAustriaCredentialAttributeTranslator
import data.credentials.MobileDrivingLicenceCredentialAttributeTranslator
import data.credentials.PowerOfRepresentationCredentialAttributeTranslator
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
    is EPrescriptionScheme -> stringResource(Res.string.credential_scheme_label_eprescription)
    else -> this?.identifier ?: "unknown"
}

suspend fun ConstantIndex.CredentialScheme?.uiLabelNonCompose(): String = when (this) {
    is IdAustriaScheme -> getString(Res.string.credential_scheme_label_id_austria)
    is EuPidScheme -> getString(Res.string.credential_scheme_label_eu_pid)
    is MobileDrivingLicenceScheme -> getString(Res.string.credential_scheme_label_mdl)
    is PowerOfRepresentationScheme -> getString(Res.string.credential_scheme_label_power_of_representation)
    is CertificateOfResidenceScheme -> getString(Res.string.credential_scheme_label_certificate_of_residence)
    is EPrescriptionScheme -> getString(Res.string.credential_scheme_label_eprescription)
    else -> this?.identifier ?: "unknown"
}

@Composable
fun ConstantIndex.CredentialScheme?.iconLabel(): String = when (this) {
    is IdAustriaScheme -> stringResource(Res.string.credential_scheme_icon_label_id_austria)
    is EuPidScheme -> stringResource(Res.string.credential_scheme_icon_label_eu_pid)
    is MobileDrivingLicenceScheme -> stringResource(Res.string.credential_scheme_icon_label_mdl)
    is PowerOfRepresentationScheme -> stringResource(Res.string.credential_scheme_icon_label_power_of_representation)
    is CertificateOfResidenceScheme -> stringResource(Res.string.credential_scheme_icon_label_certificate_of_residence)
    is EPrescriptionScheme -> stringResource(Res.string.credential_scheme_icon_label_eprescription)
    else -> this?.identifier ?: "unknown"
}

fun ConstantIndex.CredentialScheme.getLocalization(path: NormalizedJsonPath): StringResource? = when (this) {
    is MobileDrivingLicenceScheme -> { MobileDrivingLicenceCredentialAttributeTranslator.translate(path) }
    is CertificateOfResidenceScheme -> { CertificateOfResidenceCredentialAttributeTranslator.translate(path) }
    is PowerOfRepresentationScheme -> { PowerOfRepresentationCredentialAttributeTranslator.translate(path) }
    is EPrescriptionScheme -> { EPrescriptionCredentialAttributeTranslator.translate(path) }
    is EuPidScheme -> { EuPidCredentialAttributeTranslator.translate(path) }
    is IdAustriaScheme -> { IdAustriaCredentialAttributeTranslator.translate(path) }
    else -> { IdAustriaCredentialAttributeTranslator.translate(path) }
}
