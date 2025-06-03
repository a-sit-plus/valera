package ui.views.iso.verifier

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.heading_label_check_scan_qr_code
import at.asitplus.valera.resources.info_text_missing_permission
import at.asitplus.wallet.app.common.iso.transfer.DeviceEngagementMethods
import org.jetbrains.compose.resources.stringResource
import ui.composables.Logo
import ui.composables.buttons.NavigateUpButton
import ui.viewmodels.iso.VerifierViewModel
import ui.views.CameraView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifierQrEngagementView(
    navigateUp: () -> Unit,
    onClickLogo: () -> Unit,
    onFoundPayload: (String) -> Unit,
) {

    var hasPermissions by remember { mutableStateOf(false) }
    if (!hasPermissions) {
        Text(stringResource(Res.string.info_text_missing_permission))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(Res.string.heading_label_check_scan_qr_code),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Logo(onClick = onClickLogo)
                    }
                },
                navigationIcon = { NavigateUpButton(navigateUp) }
            )
        }
    ) {
        Column(modifier = Modifier.padding(it).fillMaxSize()) {
            CameraView(
                onFoundPayload = onFoundPayload,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
