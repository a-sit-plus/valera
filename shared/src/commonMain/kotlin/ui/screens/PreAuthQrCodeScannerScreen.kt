package ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ui.composables.buttons.NavigateUpButton
import ui.views.CameraView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreAuthQrCodeScannerScreen(
    onFoundPayload: (String) -> Unit,
    navigateUp: (() -> Unit)? = null,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "QR Scanner", // TODO: Add proper resource
                            style = MaterialTheme.typography.headlineLarge,
                        )
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
        Column(modifier = Modifier.padding(it).fillMaxSize()) {
            CameraView(
                onFoundPayload = onFoundPayload,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}