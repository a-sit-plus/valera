package ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.asitplus.wallet.app.common.WalletMain
import composewalletapp.shared.generated.resources.heading_label_log_screen
import composewalletapp.shared.generated.resources.Res
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import ui.composables.buttons.NavigateUpButton
import ui.composables.buttons.ShareButton

@Composable
fun LogScreen(
    navigateUp: () -> Unit,
    walletMain: WalletMain,
) {
    val logArray = try {
        walletMain.getLog()
    } catch (e: Throwable) {
        walletMain.errorService.emit(e)
        listOf()
    }

    LogView(
        logArray = logArray,
        navigateUp = navigateUp,
        shareLog = {
            walletMain.scope.launch {
                walletMain.platformAdapter.shareLog()
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun LogView(
    logArray: List<String>,
    navigateUp: () -> Unit,
    shareLog: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.heading_label_log_screen),
                        style = MaterialTheme.typography.headlineLarge,
                    )
                },
                navigationIcon = {
                    NavigateUpButton(navigateUp)
                },
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ShareButton(shareLog)
                }
            }
        }
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier.padding(scaffoldPadding).fillMaxSize(),
        ) {
            LazyColumn {
                items(logArray.size) {
                    val color: Color
                    color = if (it % 2 == 0) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                    Text(
                        text = logArray[it].trimEnd('\n'),
                        modifier = Modifier.background(color = color).padding(5.dp).fillMaxWidth(),
                        fontSize = 8.sp,
                        lineHeight = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

