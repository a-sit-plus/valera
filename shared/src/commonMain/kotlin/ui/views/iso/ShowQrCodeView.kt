package ui.views.iso

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.heading_label_show_qr_code_screen
import at.asitplus.valera.resources.info_text_qr_code_loading
import at.asitplus.wallet.app.common.iso.transfer.BluetoothInfo
import io.github.aakira.napier.Napier
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlinx.coroutines.launch
import kotlinx.io.bytestring.ByteString
import org.jetbrains.compose.resources.stringResource
import org.multipaz.compose.permissions.rememberBluetoothPermissionState
import org.multipaz.util.toBase64Url
import ui.composables.Logo
import ui.composables.buttons.NavigateUpButton
import ui.viewmodels.authentication.PresentationStateModel
import ui.viewmodels.iso.ShowQrCodeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowQrCodeView(vm: ShowQrCodeViewModel) {
    val tag = "ShowQrCodeView"

    val vm = remember { vm }
    val coroutineScope = vm.walletMain.scope
    val blePermissionState = rememberBluetoothPermissionState()
    val showQrCode = remember { mutableStateOf<ByteString?>(null) }
    val isBluetoothEnabled = BluetoothInfo().isBluetoothEnabled()

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
                        Logo(onClick = vm.onClickLogo)
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
                if (!isBluetoothEnabled) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // TODO: Add string resource for that
                        Text("Bluetooth is not available, please turn it on.")
                    }
                }
                else if (showQrCode.value != null && vm.presentationStateModel.state.collectAsState().value != PresentationStateModel.State.PROCESSING) {
                    val deviceEngagementQrCode = "mdoc:" + showQrCode.value!!.toByteArray().toBase64Url()
                    Napier.d("qrCode = \n$deviceEngagementQrCode", tag = tag)
                    Image(
                        painter = rememberQrCodePainter(deviceEngagementQrCode),
                        contentDescription = null,
                        modifier = Modifier.padding(16.dp)
                    )
                } else if (!blePermissionState.isGranted) {
                    Napier.d("ble permission is not granted", tag = tag)
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        coroutineScope.launch {
                            Napier.d("blePermissionState.launchPermissionRequest()", tag = tag)
                            blePermissionState.launchPermissionRequest()
                        }
                        // TODO handle case when user needs to go to settings application to grant permission
                    }
                } else if (blePermissionState.isGranted) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        LaunchedEffect(showQrCode.value) {
                            // TODO This block is called twice, which resets the presentationStateModel
                            if (vm.hasBeenCalledHack) return@LaunchedEffect
                            vm.hasBeenCalledHack = true

                            vm.presentationStateModel.reset()
                            vm.presentationStateModel.init()
                            vm.presentationStateModel.start(needBluetooth = true)
                            vm.presentationStateModel.setPermissionState(true)
                            vm.presentationStateModel.presentmentScope.launch {
                                vm.doHolderFlow(showQrCode)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(Res.string.info_text_qr_code_loading))
                    }
                }
            }
        }
    }
}
