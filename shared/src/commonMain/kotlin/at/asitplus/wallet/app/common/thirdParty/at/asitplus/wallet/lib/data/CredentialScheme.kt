package at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data

import androidx.compose.runtime.Composable
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.credential_scheme_icon_label_eu_pid
import at.asitplus.valera.resources.credential_scheme_icon_label_mdl
import at.asitplus.valera.resources.credential_scheme_label_eu_pid
import at.asitplus.valera.resources.credential_scheme_label_eu_pid_sdjwt
import at.asitplus.valera.resources.credential_scheme_label_mdl
import at.asitplus.wallet.lib.data.CredentialScheme
import data.credentials.EuPidCredentialAttributeTranslator
import data.credentials.MobileDrivingLicenceCredentialAttributeTranslator
import data.credentials.SingleClaimReference
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

// EU PID / mDL keep a bespoke label + attribute translator. Everything else resolves from remote type metadata and
// is labelled generically (see CredentialScheme.metadataLabel / GenericMetadataCredentialView).

@Composable
fun CredentialScheme?.uiLabel(): String = when {
    isEuPidSdJwt -> stringResource(Res.string.credential_scheme_label_eu_pid_sdjwt)
    isEuPid -> stringResource(Res.string.credential_scheme_label_eu_pid)
    isMdl -> stringResource(Res.string.credential_scheme_label_mdl)
    else -> this?.identifier ?: "unknown"
}

suspend fun CredentialScheme?.uiLabelNonCompose(): String = when {
    isEuPidSdJwt -> getString(Res.string.credential_scheme_label_eu_pid_sdjwt)
    isEuPid -> getString(Res.string.credential_scheme_label_eu_pid)
    isMdl -> getString(Res.string.credential_scheme_label_mdl)
    else -> this?.identifier ?: "unknown"
}

@Composable
fun CredentialScheme?.iconLabel(): String = when {
    isEuPid -> stringResource(Res.string.credential_scheme_icon_label_eu_pid)
    isMdl -> stringResource(Res.string.credential_scheme_icon_label_mdl)
    else -> this?.identifier ?: "unknown"
}

fun CredentialScheme.getLocalization(path: NormalizedJsonPath): StringResource? = when {
    isEuPid -> EuPidCredentialAttributeTranslator().translate(path)
    isMdl -> MobileDrivingLicenceCredentialAttributeTranslator().translate(path)
    else -> null
}

fun CredentialScheme.getLocalization(claimReference: SingleClaimReference): StringResource? = when {
    isEuPid -> EuPidCredentialAttributeTranslator().translateSingleClaimReference(claimReference)
    isMdl -> MobileDrivingLicenceCredentialAttributeTranslator().translateSingleClaimReference(claimReference)
    else -> null
}
