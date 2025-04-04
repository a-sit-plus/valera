package ui.composables.credentials

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.error_credential_status_invalid
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.agent.representation
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.iconLabel
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.uiLabel
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.primitives.TokenStatus
import org.jetbrains.compose.resources.stringResource
import ui.composables.BigErrorText
import ui.composables.CredentialStatusState
import ui.composables.LabeledText
import ui.composables.PersonAttributeDetailCardHeading
import ui.composables.PersonAttributeDetailCardHeadingIcon


@Composable
fun ColumnScope.CredentialSelectionCardHeader(
    credentialStatusState: CredentialStatusState,
    credential: SubjectCredentialStore.StoreEntry,
    modifier: Modifier = Modifier,
) {
    PersonAttributeDetailCardHeading(
        icon = {
            PersonAttributeDetailCardHeadingIcon(credential.scheme.iconLabel())
        },
        title = {
            LabeledText(
                label = credential.representation.uiLabel(),
                text = credential.scheme.uiLabel(),
            )
        },
        actionButtons = {
            when (credentialStatusState) {
                CredentialStatusState.Loading -> CircularProgressIndicator()
                is CredentialStatusState.Success -> when (credentialStatusState.tokenStatus) {
                    TokenStatus.Invalid -> BigErrorText(stringResource(Res.string.error_credential_status_invalid))
                }
            }
        }
    )
}