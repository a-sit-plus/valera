package ui.views.iso

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.heading_label_check_scan_qr_code
import org.jetbrains.compose.resources.stringResource
import ui.composables.Logo
import ui.composables.buttons.NavigateUpButton
import ui.views.CameraView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrDeviceEngagementView(
    onFoundPayload: (String) -> Unit,
    navigateUp: () -> Unit,
) {
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
                        Logo()
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
