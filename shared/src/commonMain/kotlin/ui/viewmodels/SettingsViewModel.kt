package ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.error_feature_not_yet_available
import at.asitplus.valera.resources.snackbar_clear_log_successfully
import at.asitplus.valera.resources.snackbar_reset_app_successfully
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.data.SettingsRepository
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val walletMain: WalletMain,
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

    val presentmentUseNegotiatedHandover = settingsRepository.presentmentUseNegotiatedHandover.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )
    val presentmentBleCentralClientModeEnabled = settingsRepository.presentmentBleCentralClientModeEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )
    val presentmentBlePeripheralServerModeEnabled =
        settingsRepository.presentmentBlePeripheralServerModeEnabled.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )
    val presentmentNfcDataTransferEnabled = settingsRepository.presentmentNfcDataTransferEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )
    val readerBleL2CapEnabled = settingsRepository.readerBleL2CapEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun setPresentmentUseNegotiatedHandover(
        value: Boolean,
        completionHandler: CompletionHandler = {},
    ) = viewModelScope.launch {
        try {
            settingsRepository.set(
                presentmentUseNegotiatedHandover = value
            ) {
                completionHandler(it)
            }
        } catch (throwable: Throwable) {
            completionHandler(throwable)
        }
    }

    fun setPresentmentBleCentralClientModeEnabled(
        value: Boolean,
        completionHandler: CompletionHandler = {},
    ) = viewModelScope.launch {
        try {
            settingsRepository.set(
                presentmentBleCentralClientModeEnabled = value
            ) {
                completionHandler(it)
            }
        } catch (throwable: Throwable) {
            completionHandler(throwable)
        }
    }

    fun setPresentmentBlePeripheralServerModeEnabled(
        value: Boolean,
        completionHandler: CompletionHandler = {},
    ) = viewModelScope.launch {
        try {
            settingsRepository.set(
                presentmentBlePeripheralServerModeEnabled = value
            ) {
                completionHandler(it)
            }
        } catch (throwable: Throwable) {
            completionHandler(throwable)
        }
    }

    fun setPresentmentNfcDataTransferEnabled(
        value: Boolean,
        completionHandler: CompletionHandler = {},
    ) = viewModelScope.launch {
        try {
            settingsRepository.set(
                presentmentNfcDataTransferEnabled = value
            ) {
                completionHandler(it)
            }
        } catch (throwable: Throwable) {
            completionHandler(throwable)
        }
    }

    fun setReaderBleL2CapEnabled(
        value: Boolean,
        completionHandler: CompletionHandler = {},
    ) = viewModelScope.launch {
        try {
            settingsRepository.set(
                readerBleL2CapEnabled = value
            ) {
                completionHandler(it)
            }
        } catch (throwable: Throwable) {
            completionHandler(throwable)
        }
    }
}