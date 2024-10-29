package ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.button_label_ok
import compose_wallet_app.shared.generated.resources.heading_label_error_screen
import compose_wallet_app.shared.generated.resources.info_text_error_occurred
import compose_wallet_app.shared.generated.resources.info_text_to_start_screen
import org.jetbrains.compose.resources.stringResource
import ui.composables.TextIconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorView(
    resetStack: () -> Unit,
    message: String?,
    cause: String?
) {
    val message = message ?: "Unknown Message"
    val cause = cause ?: "Unknown Cause"

    val tint = Color(255, 210, 0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.heading_label_error_screen),
                        style = MaterialTheme.typography.headlineLarge,
                    )
                },
                navigationIcon = {}
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(
                    BottomAppBarDefaults.ContainerElevation
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.info_text_error_occurred),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(Res.string.info_text_to_start_screen),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        TextIconButton(
                            icon = {},
                            text = {
                                Text(stringResource(Res.string.button_label_ok))
                            },
                            onClick = {
                                resetStack()
                            },
                        )
                    }
                }
            }
        }
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier.padding(scaffoldPadding).fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp).fillMaxSize()
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    Modifier.size(100.dp),
                    tint = tint
                )
                Text("Message:", fontWeight = FontWeight.Bold)
                Column(
                    modifier = Modifier.heightIn(max = 150.dp)
                        .background(color = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Text(
                        message,
                        modifier = Modifier.padding(
                            top = 5.dp,
                            bottom = 5.dp,
                            start = 10.dp,
                            end = 10.dp
                        ).fillMaxWidth().verticalScroll(
                            rememberScrollState()
                        ),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.size(5.dp))
                Text("Cause:", fontWeight = FontWeight.Bold)
                Column(
                    modifier = Modifier.heightIn(max = 150.dp)
                        .background(color = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Text(
                        cause,
                        modifier = Modifier.padding(
                            top = 5.dp,
                            bottom = 5.dp,
                            start = 10.dp,
                            end = 10.dp
                        ).fillMaxWidth().verticalScroll(
                            rememberScrollState()
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
