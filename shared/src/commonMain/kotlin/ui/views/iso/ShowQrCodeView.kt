package ui.views.iso

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.heading_label_show_qr_code_screen
import at.asitplus.valera.resources.info_text_missing_permission
import at.asitplus.valera.resources.info_text_qr_code_loading
import org.jetbrains.compose.resources.stringResource
import qrcode.QRCode
import ui.composables.Logo
import ui.composables.buttons.NavigateUpButton
import ui.viewmodels.iso.ShowQrCodeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowQrCodeView(vm: ShowQrCodeViewModel) {

    val vm = remember { vm }
    val holder = vm.holder

    if (!vm.permission) {
        holder.getRequirements { b -> vm.permission = b }
    }

    LaunchedEffect(Unit) {
        val updateQrCode: (String) -> Unit = { str -> vm.qrcodeText = str }
        holder.hold(updateQrCode) {
            vm.shouldDisconnect = false
            vm.onConnection(holder)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (vm.shouldDisconnect) {
                holder.disconnect()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(Res.string.heading_label_show_qr_code_screen),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Logo()
                    }
                },
                navigationIcon = { NavigateUpButton(vm.navigateUp) }
            )
        }
    ) { scaffoldPadding ->
        Box(modifier = Modifier.padding(scaffoldPadding)) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!vm.permission) {
                    Text(stringResource(Res.string.info_text_missing_permission))
                } else if (vm.qrcodeText.isEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(Res.string.info_text_qr_code_loading))
                    }
                } else {
                    val qrCode = createQrCode(vm.qrcodeText)
                    val imageBitmap = vm.walletMain.platformAdapter.decodeImage(qrCode)
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = null
                    )
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
