package ui.viewmodels.iso.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.asitplus.wallet.app.common.data.SettingsRepository
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransferOptionsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val presentmentUseNegotiatedHandover =
        settingsRepository.presentmentUseNegotiatedHandover.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )
    val presentmentBleCentralClientModeEnabled =
        settingsRepository.presentmentBleCentralClientModeEnabled.stateIn(
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
    val presentmentNfcDataTransferEnabled =
        settingsRepository.presentmentNfcDataTransferEnabled.stateIn(
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
