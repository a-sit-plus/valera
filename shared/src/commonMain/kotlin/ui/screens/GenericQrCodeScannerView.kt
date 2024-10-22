package ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import ui.composables.buttons.NavigateUpButton
import ui.views.CameraView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericQrCodeScannerView(
    title: String,
    subTitle: String?,
    navigateUp: (() -> Unit)?,
    onFoundQrCode: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            title,
                            style = MaterialTheme.typography.headlineLarge,
                        )
                        if (subTitle != null) {
                            Text(
                                subTitle,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
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
        CameraView(
            onFoundPayload = onFoundQrCode,
            modifier = Modifier.fillMaxSize(),
        )
    }
}