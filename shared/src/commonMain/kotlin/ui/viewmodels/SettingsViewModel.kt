package ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.snackbar_clear_log_successfully
import at.asitplus.valera.resources.snackbar_reset_app_successfully
import at.asitplus.wallet.app.common.WalletMain
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

class SettingsViewModel(
    private val walletMain: WalletMain
) : ViewModel() {
    fun showGlobalSnackbar(
        message: suspend () -> String,
    ) = viewModelScope.launch {
        walletMain.snackbarService.showSnackbar(message())
    }

    fun onClickResetApp(
        completionHandler: CompletionHandler = {},
    ) = viewModelScope.launch {
        try {
            walletMain.resetApp()
            val resetMessage = getString(Res.string.snackbar_reset_app_successfully)
            walletMain.snackbarService.showSnackbar(resetMessage)
            completionHandler(null)
        } catch(throwable: Throwable) {
            walletMain.errorService.emit(throwable)
            completionHandler(throwable)
        }
    }

    fun onClickClearLogFile(
        completionHandler: CompletionHandler = {},
    ) = viewModelScope.launch {
        try {
            walletMain.clearLog()
            val clearMessage = getString(Res.string.snackbar_clear_log_successfully)
            walletMain.snackbarService.showSnackbar(clearMessage)
            completionHandler(null)
        } catch (throwable: Throwable) {
            walletMain.errorService.emit(throwable)
            completionHandler(throwable)
        }
    }
}
