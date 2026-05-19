package ui.viewmodels.iso.common

import at.asitplus.KmmResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.data.SettingsRepository
import at.asitplus.wallet.app.common.iso.transfer.method.DeviceEngagementMethods
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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

    private val bleCentralClientModeOverride = MutableStateFlow<Boolean?>(null)
    private val blePeripheralServerModeOverride = MutableStateFlow<Boolean?>(null)

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
        withOptimisticOverride(
            source = settingsRepository.presentmentBleCentralClientModeEnabled,
            override = bleCentralClientModeOverride,
        ).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val presentmentBlePeripheralServerModeEnabled =
        withOptimisticOverride(
            source = settingsRepository.presentmentBlePeripheralServerModeEnabled,
            override = blePeripheralServerModeOverride,
        ).stateIn(
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

    val bleUseL2CAPEnabled = settingsRepository.bleUseL2CAPEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val bleUseL2CAPInEngagementEnabled = settingsRepository.bleUseL2CAPInEngagementEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    private fun update(
        block: suspend SettingsRepository.() -> KmmResult<Unit>,
        onComplete: (KmmResult<Unit>) -> Unit = {},
    ) = walletMain.scope.launch {
        onComplete(settingsRepository.block())
    }

    fun setPresentmentUseNegotiatedHandover(value: Boolean) =
        update(block = { set(presentmentUseNegotiatedHandover = value) })

    fun setPresentmentBleCentralClientModeEnabled(value: Boolean) {
        bleCentralClientModeOverride.value = value
        update(
            block = { settingsRepository.setPresentmentBleCentralClientModeEnabled(value) },
            onComplete = { result ->
                if (result.isFailure) {
                    bleCentralClientModeOverride.value = null
                }
            }
        )
        clearBleOverrideWhenPersisted(
            override = bleCentralClientModeOverride,
            source = settingsRepository.presentmentBleCentralClientModeEnabled,
            expectedValue = value,
        )
    }

    fun setPresentmentBlePeripheralServerModeEnabled(value: Boolean) {
        blePeripheralServerModeOverride.value = value
        update(
            block = { settingsRepository.setPresentmentBlePeripheralServerModeEnabled(value) },
            onComplete = { result ->
                if (result.isFailure) {
                    blePeripheralServerModeOverride.value = null
                }
            }
        )
        clearBleOverrideWhenPersisted(
            override = blePeripheralServerModeOverride,
            source = settingsRepository.presentmentBlePeripheralServerModeEnabled,
            expectedValue = value,
        )
    }

    fun setPresentmentBleEnabled(value: Boolean) {
        if (!value) {
            bleCentralClientModeOverride.value = false
            blePeripheralServerModeOverride.value = false
            update(
                block = { settingsRepository.setPresentmentBleEnabled(false) },
                onComplete = { result ->
                    if (result.isFailure) {
                        bleCentralClientModeOverride.value = null
                        blePeripheralServerModeOverride.value = null
                    }
                }
            )
            clearBleOverrideWhenPersisted(
                override = bleCentralClientModeOverride,
                source = settingsRepository.presentmentBleCentralClientModeEnabled,
                expectedValue = false,
            )
            clearBleOverrideWhenPersisted(
                override = blePeripheralServerModeOverride,
                source = settingsRepository.presentmentBlePeripheralServerModeEnabled,
                expectedValue = false,
            )
        } else {
            update(block = { settingsRepository.setPresentmentBleEnabled(true) })
        }
    }

    fun setPresentmentNfcDataTransferEnabled(value: Boolean) =
        update(block = { set(presentmentNfcDataTransferEnabled = value) })

    fun setBleL2CAPEnabled(value: Boolean) = update(block = { set(bleUseL2CAPEnabled = value) })

    fun setBleL2CAPInEngagementEnabled(value: Boolean) =
        update(block = { set(bleUseL2CAPInEngagementEnabled = value) })

    private fun withOptimisticOverride(
        source: Flow<Boolean>,
        override: StateFlow<Boolean?>,
    ): Flow<Boolean> = combine(source, override) { persisted, pending ->
        pending ?: persisted
    }

    private fun clearBleOverrideWhenPersisted(
        override: MutableStateFlow<Boolean?>,
        source: Flow<Boolean>,
        expectedValue: Boolean,
    ) {
        walletMain.scope.launch {
            val persistedValue = source.first { it == expectedValue }
            if (override.value == persistedValue) {
                override.value = null
            }
        }
    }
}
