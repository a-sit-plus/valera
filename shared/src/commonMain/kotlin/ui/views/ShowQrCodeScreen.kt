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
import data.bletransfer.verifier.ReceivedDocument
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
    var requestedAttributes by remember { mutableStateOf(listOf<ReceivedDocument>()) }

    if (!permission) {
        holder.getRequirements { b -> permission = b }
    }

    LaunchedEffect(Unit) {
        val updateQrCode: (String) -> Unit =  { str -> qrcodeText = str }
        val updateRequestedAttributes: (List<ReceivedDocument>) -> Unit = {l -> requestedAttributes = l}

        holder.hold(updateQrCode, updateRequestedAttributes)
    }

    DisposableEffect(Unit) {
        onDispose {
            holder.disconnect()
        }
    }


    if (requestedAttributes.isEmpty()) {
        QrCodeView(walletMain, permission, qrcodeText)
    } else {
        TODO("Not Implemented Yet")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrCodeView(walletMain: WalletMain, permission: Boolean, qrcodeText: String) {
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
    return QRCode.ofSquares()
        .build(str)
        .renderToBytes()
}

