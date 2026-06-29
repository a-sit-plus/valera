package ui.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import ui.composables.ScreenHeading
import ui.composables.buttons.NavigateUpButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericQrCodeScannerView(
    title: String,
    subTitle: String?,
    navigateUp: (() -> Unit)?,
    onFoundQrCode: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            ScreenHeading(title)
                            if (subTitle != null) {
                                Text(
                                    subTitle,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    if(navigateUp != null) {
                        NavigateUpButton(navigateUp)
                    }
                },
            )
        },
    ) {
        Column {
            CameraView(
                onFoundPayload = onFoundQrCode,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
