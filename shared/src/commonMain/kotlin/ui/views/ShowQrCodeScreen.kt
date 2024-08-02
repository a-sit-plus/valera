package ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.app.common.WalletMain
import composewalletapp.shared.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import composewalletapp.shared.generated.resources.heading_label_show_qr_code_screen
import data.bletransfer.Holder
import data.bletransfer.getHolder
import qrcode.QRCode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowQrCodeScreen(walletMain: WalletMain) {
    ShowQrCodeView(
        walletMain = walletMain
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowQrCodeView(walletMain: WalletMain) {

    val holder: Holder = remember { getHolder() }
    var permission by remember { mutableStateOf(false) }
    var qrcodeText by remember { mutableStateOf("") }

    if (!permission) {
        holder.getRequirements { b -> permission = b }
    }

    LaunchedEffect(Unit) {
        /*val updateLog: (String?, String) -> Unit = { TAG, updateLog ->
            logsState += updateLog
            Napier.d(tag= TAG?: "DebugScreen", message = updateLog)
        }

        val updateData: (List<Entry>) -> Unit = { entry ->
            entryState += entry
        }

        updateLog(null, "Start Connection and Data Transfer")
        verifier.verify(payload, document, updateLog, updateData)*/

        val updateQrCode: (String) -> Unit =  { str -> qrcodeText = str }

        holder.hold(updateQrCode)
    }

    DisposableEffect(Unit) {
        onDispose {
            holder.disconnect()
        }
    }



    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.heading_label_show_qr_code_screen),
                        style = MaterialTheme.typography.headlineLarge
                    )
                },
            )
        }
    ) { scaffoldPadding ->
        Box(modifier = Modifier.padding(scaffoldPadding)) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (!permission) {
                    Text("Permission Denied")
                } else if (qrcodeText.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading Qr Code")
                    }
                } else {
                    val qrCode = createQrCode(qrcodeText)
                    val imageBitmap = walletMain.platformAdapter.decodeImage(qrCode)
                    Image(bitmap = imageBitmap, contentDescription = null)
                }

            }
        }
    }
}



fun createQrCode(str: String): ByteArray {
    //val str = "mdoc:owBjMS4wAYIB2BhYS6QBAiABIVgg9wnmwlhryhSiEzpZawihe23JSm-7uJeaEo1iQ1LB7EMiWCDCxVkZmTHMopRblja1qqn-4ivazuL_v9rj3UoRK-t2KQKBgwIBowD0AfULULgeyOTLZkkDpUDM72qV7Rw"

    return QRCode.ofSquares()
        .build(str)
        .renderToBytes()
    /*val squareQRCode = QRCode.ofSquares()
        .build(str)
    val squarePngData = squareQRCode.renderToBytes()

    return squarePngData*/
}

