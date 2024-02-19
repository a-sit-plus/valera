package ui.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import navigation.Page


class AuthenticationQrCodeScannerPage : Page

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticationQrCodeScannerView(
    navigateUp: () -> Unit,
    navigateToConsentScreenWithResult: (spName: String, spLocation: String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Anmelden",
                            style = MaterialTheme.typography.headlineLarge,
                        )
                        Text(
                            "an Schalter oder Maschine",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = navigateUp
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Navigate Back",
                        )
                    }
                },
            )
        },
    ) {
        Box(modifier = Modifier.padding(it)) {
        }
    }
}