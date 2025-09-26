package ui.views.iso.common

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_back_to_settings
import at.asitplus.valera.resources.button_label_open_device_settings
import at.asitplus.valera.resources.info_text_nfc_engagement_not_available

import org.jetbrains.compose.resources.stringResource
import ui.composables.TextIconButton

@Composable
fun NfcEngagementNotAvailableView(
    onClickBackToSettings: () -> Unit,
    onOpenDeviceSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    CenteredInfoText(
        message = stringResource(Res.string.info_text_nfc_engagement_not_available),
        modifier = modifier
    ) {
        TextIconButton(
            icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
            text = { Text(stringResource(Res.string.button_label_back_to_settings)) },
            onClick = onClickBackToSettings
        )
        Spacer(Modifier.height(8.dp))
        TextIconButton(
            icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
            text = { Text(stringResource(Res.string.button_label_open_device_settings)) },
            onClick = onOpenDeviceSettings
        )
    }
}
