@file:OptIn(ExperimentalResourceApi::class)

package ui.composables.buttons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.button_label_consent
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import ui.composables.TextIconButton

@OptIn(ExperimentalResourceApi::class)
@Composable
fun ConsentButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextIconButton(
        icon = {
            Icon(
                imageVector = Icons.Default.Done,
                contentDescription = null,
            )
        },
        text = {
            Text(stringResource(Res.string.button_label_consent))
        },
        onClick = onClick,
        modifier = modifier,
    )
}