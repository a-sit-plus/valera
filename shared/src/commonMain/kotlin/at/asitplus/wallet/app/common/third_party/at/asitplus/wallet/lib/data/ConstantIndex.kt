package at.asitplus.wallet.app.common.third_party.at.asitplus.wallet.lib.data

import androidx.compose.runtime.Composable
import at.asitplus.wallet.lib.data.ConstantIndex
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.credential_representation_format_label_mso_mdoc
import compose_wallet_app.shared.generated.resources.credential_representation_format_label_plain_jwt
import compose_wallet_app.shared.generated.resources.credential_representation_format_label_sd_jwt
import org.jetbrains.compose.resources.stringResource

val ConstantIndex.CredentialScheme.identifier: String
    get() = vcType ?: isoDocType ?: sdJwtType ?: schemaUri

@Composable
fun ConstantIndex.CredentialRepresentation.uiLabel(): String = when (this) {
    ConstantIndex.CredentialRepresentation.PLAIN_JWT -> stringResource(Res.string.credential_representation_format_label_plain_jwt)
    ConstantIndex.CredentialRepresentation.SD_JWT -> stringResource(Res.string.credential_representation_format_label_sd_jwt)
    ConstantIndex.CredentialRepresentation.ISO_MDOC -> stringResource(Res.string.credential_representation_format_label_mso_mdoc)
}