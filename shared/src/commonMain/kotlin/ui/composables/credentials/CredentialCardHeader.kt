package ui.composables.credentials

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.agent.representation
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.iconLabel
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.uiLabel
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.primitives.TokenStatus
import ui.composables.CredentialCardActionMenu
import ui.composables.LabeledText
import ui.composables.PersonAttributeDetailCardHeading
import ui.composables.PersonAttributeDetailCardHeadingIcon


@Composable
fun ColumnScope.CredentialCardHeader(
    credential: SubjectCredentialStore.StoreEntry,
    tokenStatus: TokenStatus?,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val status = when(credential) {
        is SubjectCredentialStore.StoreEntry.Iso -> credential.issuerSigned.issuerAuth.payload?.status
        is SubjectCredentialStore.StoreEntry.SdJwt -> credential.sdJwt.credentialStatus
        is SubjectCredentialStore.StoreEntry.Vc -> credential.vc.vc.credentialStatus
    }

    PersonAttributeDetailCardHeading(
        icon = {
            PersonAttributeDetailCardHeadingIcon(credential.scheme.iconLabel())
        },
        title = {
            LabeledText(
                label = credential.representation.uiLabel(),
                text = "(${tokenStatus.uiLabel()} ${status?.statusList?.index}) " + credential.scheme.uiLabel(),
            )
        },
    ) {
        CredentialCardActionMenu(
            onDelete = onDelete
        )
    }
}

private fun TokenStatus?.uiLabel() = when(this?.value) {
    null -> "STATUS UNKNOWN"
    0u.toUByte() -> "VALID"
    1u.toUByte() -> "INVALID"
    else -> "STATUS UNKNOWN"
}