package at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data

import androidx.compose.runtime.Composable
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.credential_scheme_icon_label_eu_pid
import at.asitplus.valera.resources.credential_scheme_icon_label_mdl
import at.asitplus.valera.resources.credential_scheme_label_eu_pid
import at.asitplus.valera.resources.credential_scheme_label_eu_pid_sdjwt
import at.asitplus.valera.resources.credential_scheme_label_mdl
import at.asitplus.wallet.app.common.CredentialMetadataDisplayNames
import at.asitplus.wallet.lib.data.CredentialScheme
import data.credentials.EuPidCredentialAttributeTranslator
import data.credentials.MobileDrivingLicenceCredentialAttributeTranslator
import data.credentials.SingleClaimReference
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

// EU PID / mDL keep a bespoke label + attribute translator. Everything else resolves from remote type metadata and
// is labelled generically (see CredentialScheme.metadataLabel / GenericMetadataCredentialView).

/** Display name from resolved type metadata, falling back to the identifier (vct/docType). */
private val CredentialScheme?.metadataDisplayName: String?
    get() = CredentialMetadataDisplayNames[this?.schemaUri]

@Composable
fun CredentialScheme?.uiLabel(): String = when {
    isEuPidSdJwt -> stringResource(Res.string.credential_scheme_label_eu_pid_sdjwt)
    isEuPid -> stringResource(Res.string.credential_scheme_label_eu_pid)
    isMdl -> stringResource(Res.string.credential_scheme_label_mdl)
    else -> metadataDisplayName ?: this?.identifier ?: "unknown"
}

suspend fun CredentialScheme?.uiLabelNonCompose(): String = when {
    isEuPidSdJwt -> getString(Res.string.credential_scheme_label_eu_pid_sdjwt)
    isEuPid -> getString(Res.string.credential_scheme_label_eu_pid)
    isMdl -> getString(Res.string.credential_scheme_label_mdl)
    else -> metadataDisplayName ?: this?.identifier ?: "unknown"
}

@Composable
fun CredentialScheme?.iconLabel(): String = when {
    isEuPid -> stringResource(Res.string.credential_scheme_icon_label_eu_pid)
    isMdl -> stringResource(Res.string.credential_scheme_icon_label_mdl)
    // Initials of the credential name (or vct) for the small round icon, rather than the full identifier.
    else -> (metadataDisplayName ?: this?.identifier)
        ?.split(' ', '-', ':', '.')?.filter { it.isNotBlank() }
        ?.take(2)?.mapNotNull { it.firstOrNull()?.uppercaseChar() }?.joinToString("")
        ?.ifEmpty { null } ?: "?"
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
