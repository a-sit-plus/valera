package at.asitplus.wallet.app.common.third_party.at.asitplus.wallet.lib.data

import androidx.compose.runtime.Composable
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import at.asitplus.wallet.por.PowerOfRepresentationScheme
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.credential_scheme_icon_label_certificate_of_residence
import composewalletapp.shared.generated.resources.credential_scheme_icon_label_eu_pid
import composewalletapp.shared.generated.resources.credential_scheme_icon_label_id_austria
import composewalletapp.shared.generated.resources.credential_scheme_icon_label_mdl
import composewalletapp.shared.generated.resources.credential_scheme_icon_label_power_of_representation
import composewalletapp.shared.generated.resources.credential_scheme_label_certificate_of_residence
import composewalletapp.shared.generated.resources.credential_scheme_label_eu_pid
import composewalletapp.shared.generated.resources.credential_scheme_label_id_austria
import composewalletapp.shared.generated.resources.credential_scheme_label_mdl
import composewalletapp.shared.generated.resources.credential_scheme_label_power_of_representation
import org.jetbrains.compose.resources.stringResource

@Composable
fun ConstantIndex.CredentialScheme.uiLabel(): String = when (this) {
    is IdAustriaScheme -> stringResource(Res.string.credential_scheme_label_id_austria)
    is EuPidScheme -> stringResource(Res.string.credential_scheme_label_eu_pid)
    is MobileDrivingLicenceScheme -> stringResource(Res.string.credential_scheme_label_mdl)
    is CertificateOfResidenceScheme -> stringResource(Res.string.credential_scheme_label_power_of_representation)
    is PowerOfRepresentationScheme -> stringResource(Res.string.credential_scheme_label_certificate_of_residence)
    else -> this.identifier
}

@Composable
fun ConstantIndex.CredentialScheme.iconLabel(): String = when (this) {
    is IdAustriaScheme -> stringResource(Res.string.credential_scheme_icon_label_id_austria)
    is EuPidScheme -> stringResource(Res.string.credential_scheme_icon_label_eu_pid)
    is MobileDrivingLicenceScheme -> stringResource(Res.string.credential_scheme_icon_label_mdl)
    is CertificateOfResidenceScheme -> stringResource(Res.string.credential_scheme_icon_label_power_of_representation)
    is PowerOfRepresentationScheme -> stringResource(Res.string.credential_scheme_icon_label_certificate_of_residence)
    else -> this.identifier
}