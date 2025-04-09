package ui.viewmodels

import androidx.compose.foundation.text.selection.DisableSelection
import at.asitplus.wallet.app.common.WalletMain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

class SigningQtspSelectionViewModel(
    val navigateUp: () -> Unit,
    val onContinue: (String) -> Unit,
    val walletMain: WalletMain,
    val onClickLogo: () -> Unit,
    val onClickSettings: () -> Unit,
    val url: String
) {
    val onClickPreload: () -> Unit = {
        CoroutineScope(Dispatchers.IO).launch {
            walletMain.signingService.preloadCertificate()
        }
    }
}
