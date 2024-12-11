@file:OptIn(ExperimentalResourceApi::class)

package ui.composables.buttons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_provision_credential_qr
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import ui.composables.TextIconButton

@Composable
fun LoadDataQrButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextIconButton(
        icon = {
            Icon(
                imageVector = Icons.Default.QrCode,
                contentDescription = null,
            )
        },
        text = {
            Text(stringResource(Res.string.button_label_provision_credential_qr))
        },
        onClick = onClick,
        modifier = modifier,
    )
}