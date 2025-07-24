package ui.viewmodels

import androidx.compose.runtime.mutableStateOf
import at.asitplus.rqes.SignatureRequestParameters
import at.asitplus.wallet.app.common.WalletMain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SigningQtspSelectionViewModel(
    val navigateUp: () -> Unit,
    val onContinue: (SignatureRequestParameters) -> Unit,
    val walletMain: WalletMain,
    val onClickLogo: () -> Unit,
    val onClickSettings: () -> Unit,
    val signatureRequestParameters: SignatureRequestParameters
) {
    val selection = mutableStateOf(walletMain.signingService.config.current)
    val credentialInfo =
        mutableStateOf(walletMain.signingService.config.getQtspByIdentifier(selection.value).credentialInfo)
    val qtspList = walletMain.signingService.config.qtsps

    val onClickPreload: () -> Unit = {
        CoroutineScope(Dispatchers.IO).launch {
            walletMain.signingService.preloadCertificate()
        }
    }

    val onClickDelete: () -> Unit = {
        walletMain.signingService.config.getQtspByIdentifier(selection.value).credentialInfo = null
        credentialInfo.value = null
        runBlocking { walletMain.signingService.exportToDataStore() }
    }

    val onQtspChange: (String) -> Unit = { qtsp ->
        CoroutineScope(Dispatchers.IO).launch {
            selection.value = qtsp
            credentialInfo.value = walletMain.signingService.config.getQtspByIdentifier(selection.value).credentialInfo
            walletMain.signingService.setCurrentQtsp(qtsp)
        }
    }

    fun allowPreload(): Boolean = walletMain.signingService.config.getQtspByIdentifier(selection.value).allowPreload
}
