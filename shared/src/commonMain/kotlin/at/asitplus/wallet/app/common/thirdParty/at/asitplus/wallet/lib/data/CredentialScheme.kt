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
import data.credentials.CredentialAttributeTranslator
import data.credentials.SingleClaimReference
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

// Claim labels come from resolved type metadata; only scheme-level labels remain local resources for known cards.

/** Display name from resolved type metadata, falling back to the identifier (vct/docType). */
private val CredentialScheme?.metadataDisplayName: String?
    get() = CredentialMetadataDisplayNames[this?.identifier]

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

fun CredentialScheme.getLocalization(path: NormalizedJsonPath): String? =
    CredentialAttributeTranslator[this]?.translate(path)

fun CredentialScheme.getLocalization(claimReference: SingleClaimReference): String? =
    CredentialAttributeTranslator[this]?.translateSingleClaimReference(claimReference)
