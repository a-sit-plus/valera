package at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data

import androidx.compose.runtime.Composable
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.credential_representation_format_label_mso_mdoc
import at.asitplus.valera.resources.credential_representation_format_label_plain_jwt
import at.asitplus.valera.resources.credential_representation_format_label_sd_jwt
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation
import at.asitplus.wallet.lib.data.CredentialScheme
import org.jetbrains.compose.resources.stringResource

val CredentialScheme.identifier: String
    get() = vcType ?: isoDocType ?: sdJwtType ?: "unknown"

@Composable
fun CredentialRepresentation.uiLabel(): String = when (this) {
    CredentialRepresentation.PLAIN_JWT -> stringResource(Res.string.credential_representation_format_label_plain_jwt)
    CredentialRepresentation.SD_JWT -> stringResource(Res.string.credential_representation_format_label_sd_jwt)
    CredentialRepresentation.ISO_MDOC -> stringResource(Res.string.credential_representation_format_label_mso_mdoc)
}