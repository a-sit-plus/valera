package ui.composables.credentials

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.error_credential_status_invalid
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.primitives.TokenStatus
import org.jetbrains.compose.resources.stringResource
import ui.composables.BigErrorText
import ui.composables.buttons.DetailsButton


@Composable
fun ColumnScope.CredentialCardFooter(
    isTokenStatusEvaluated: Boolean,
    tokenStatus: TokenStatus?,
    onOpenDetails: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    onOpenDetails?.let {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier,
        ) {
            Box {
                when {
                    tokenStatus?.isInvalid == true -> BigErrorText(stringResource(Res.string.error_credential_status_invalid))
                }
            }
            DetailsButton(
                onClick = onOpenDetails
            )
        }
    }
}