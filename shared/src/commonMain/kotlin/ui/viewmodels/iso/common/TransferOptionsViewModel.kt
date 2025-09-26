package ui.viewmodels.iso.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.data.SettingsRepository
import at.asitplus.wallet.app.common.iso.transfer.method.DeviceEngagementMethods
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

open class TransferOptionsViewModel(
    val walletMain: WalletMain,
    val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _settingsReady = MutableStateFlow(false)
    val settingsReady: StateFlow<Boolean> = _settingsReady.asStateFlow()

    fun initSettings() {
        if (_settingsReady.value) return
        walletMain.scope.launch {
            settingsRepository.awaitPresentmentSettingsFirst()
            _settingsReady.value = true
        }
    }

    private val _selectedEngagementMethod = MutableStateFlow(DeviceEngagementMethods.QR_CODE)
    val selectedEngagementMethod: StateFlow<DeviceEngagementMethods> = _selectedEngagementMethod

    fun setEngagementMethod(method: DeviceEngagementMethods) {
        if(_selectedEngagementMethod.value == method) return
        _selectedEngagementMethod.value = method
    }

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

    private fun update(block: suspend SettingsRepository.() -> Result<Unit>) =
        viewModelScope.launch { settingsRepository.block() }

    fun setPresentmentUseNegotiatedHandover(value: Boolean) =
        update { set(presentmentUseNegotiatedHandover = value) }

    fun setPresentmentBleCentralClientModeEnabled(value: Boolean) =
        update { set(presentmentBleCentralClientModeEnabled = value) }

    fun setPresentmentBlePeripheralServerModeEnabled(value: Boolean) =
        update { set(presentmentBlePeripheralServerModeEnabled = value) }

    fun setPresentmentNfcDataTransferEnabled(value: Boolean) =
        update { set(presentmentNfcDataTransferEnabled = value) }

    fun setReaderBleL2CapEnabled(value: Boolean) =
        update { set(readerBleL2CapEnabled = value) }
}
