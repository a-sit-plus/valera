package at.asitplus.wallet.app.common.third_party.at.asitplus.wallet.lib.data

import androidx.compose.runtime.Composable
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import at.asitplus.wallet.eprescription.EPrescriptionScheme
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import at.asitplus.wallet.por.PowerOfRepresentationScheme
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.credential_scheme_icon_label_certificate_of_residence
import compose_wallet_app.shared.generated.resources.credential_scheme_icon_label_eu_pid
import compose_wallet_app.shared.generated.resources.credential_scheme_icon_label_id_austria
import compose_wallet_app.shared.generated.resources.credential_scheme_icon_label_mdl
import compose_wallet_app.shared.generated.resources.credential_scheme_icon_label_power_of_representation
import compose_wallet_app.shared.generated.resources.credential_scheme_label_certificate_of_residence
import compose_wallet_app.shared.generated.resources.credential_scheme_label_epresciption
import compose_wallet_app.shared.generated.resources.credential_scheme_label_eu_pid
import compose_wallet_app.shared.generated.resources.credential_scheme_label_id_austria
import compose_wallet_app.shared.generated.resources.credential_scheme_label_mdl
import compose_wallet_app.shared.generated.resources.credential_scheme_label_power_of_representation
import org.jetbrains.compose.resources.stringResource

@Composable
fun ConstantIndex.CredentialScheme?.uiLabel(): String = when (this) {
    is IdAustriaScheme -> stringResource(Res.string.credential_scheme_label_id_austria)
    is EuPidScheme -> stringResource(Res.string.credential_scheme_label_eu_pid)
    is MobileDrivingLicenceScheme -> stringResource(Res.string.credential_scheme_label_mdl)
    is PowerOfRepresentationScheme -> stringResource(Res.string.credential_scheme_label_power_of_representation)
    is CertificateOfResidenceScheme -> stringResource(Res.string.credential_scheme_label_certificate_of_residence)
    is EPrescriptionScheme -> stringResource(Res.string.credential_scheme_label_epresciption)
    else -> this?.identifier ?: "Unknown"
}

@Composable
fun ConstantIndex.CredentialScheme?.iconLabel(): String = when (this) {
    is IdAustriaScheme -> stringResource(Res.string.credential_scheme_icon_label_id_austria)
    is EuPidScheme -> stringResource(Res.string.credential_scheme_icon_label_eu_pid)
    is MobileDrivingLicenceScheme -> stringResource(Res.string.credential_scheme_icon_label_mdl)
    is PowerOfRepresentationScheme -> stringResource(Res.string.credential_scheme_icon_label_power_of_representation)
    is CertificateOfResidenceScheme -> stringResource(Res.string.credential_scheme_icon_label_certificate_of_residence)
    is EPrescriptionScheme -> stringResource(Res.string.credential_scheme_label_epresciption)
    else -> this?.identifier ?: "Unknown"
}
