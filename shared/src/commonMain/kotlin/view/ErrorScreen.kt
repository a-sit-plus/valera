package view

import Resources
import UncorrectableErrorException
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
import androidx.compose.material.icons.filled.Close
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
import at.asitplus.wallet.app.common.WalletMain
import ui.composables.TextIconButton
import ui.composables.buttons.NavigateUpButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorScreen(
    walletMain: WalletMain
) {
    val throwable = walletMain.errorService.throwable.value
    val message = throwable?.message ?: "Unknown Message"
    val cause = throwable?.cause?.message ?: "Unknown Cause"
    val tint = if (throwable is UncorrectableErrorException) {
        Color.Red
    } else {
        Color(255, 210, 0)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        Resources.HEADING_LABEL_ERROR_SCREEN,
                        style = MaterialTheme.typography.headlineLarge,
                    )
                },
                navigationIcon = {
                    if (throwable !is UncorrectableErrorException) {
                        NavigateUpButton(
                            onClick = {
                                walletMain.errorService.reset()
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (throwable is UncorrectableErrorException) {
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
                            text = Resources.INFO_TEXT_IRREVOCABLE_ERROR,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = Resources.INFO_TEXT_RESTART_APP,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            TextIconButton(
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                    )
                                },
                                text = {
                                    Text(Resources.BUTTON_LABEL_CLOSE_APP)
                                },
                                onClick = {
                                    walletMain.platformAdapter.exitApp()
                                },
                            )
                        }
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