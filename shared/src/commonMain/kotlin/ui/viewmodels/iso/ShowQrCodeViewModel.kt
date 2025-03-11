package ui.viewmodels.iso

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import at.asitplus.wallet.app.common.WalletMain
import qrcode.QRCode

class ShowQrCodeViewModel(
    val walletMain: WalletMain,
    val navigateUp: () -> Unit,
//    val onConnection: (Holder) -> Unit
) {


    var permission by mutableStateOf(false)
    var qrcodeText by mutableStateOf("")
    var shouldDisconnect by mutableStateOf(true)
//
//    private var transferManager: TransferManager? = null
//    private var requestedAttributes: List<RequestedDocument>? = null
//
//    @Composable
//    fun getRequirements(check: (Boolean) -> Unit) {
//        RequestBluetoothPermissions { b ->
//            permission = b
//            check(b)
//        }
//        PreferencesHelper.initialize(LocalContext.current)
//        // The following 2 lines are needed so that it works for the eAusweise app
//        // (If omitted it works with the google verifier app)
//        PreferencesHelper.setBleDataRetrievalEnabled(false)
//        PreferencesHelper.setBlePeripheralDataRetrievalMode(true)
//
//        transferManager = TransferManager.getInstance(LocalContext.current)
//    }


    // TODO: handle onConnection


    fun createQrCode(): ByteArray {
        return QRCode.ofSquares()
            .build(qrcodeText)
            .renderToBytes()
    }
}
