package ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.snackbar_clear_log_successfully
import at.asitplus.valera.resources.snackbar_reset_app_successfully
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.data.SettingsRepository
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

@OptIn(FlowPreview::class)
class SettingsViewModel(
    private val walletMain: WalletMain
) : ViewModel() {
    val clientId = walletMain.settingsRepository.clientId.stateIn(
        viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsRepository.DEFAULT_CLIENT_ID
    )

    private val clientIdInput = MutableStateFlow(SettingsRepository.DEFAULT_CLIENT_ID)
    val clientIdInputState = clientIdInput.asStateFlow()

    val defaultOpenId4VpAllowedOriginSchemes =
        walletMain.settingsRepository.defaultOpenId4VpAllowedOriginSchemes
    private val openId4VpAllowedOriginSchemesInput = MutableStateFlow(
        defaultOpenId4VpAllowedOriginSchemes.toInputString()
    )
    val openId4VpAllowedOriginSchemesInputState = openId4VpAllowedOriginSchemesInput.asStateFlow()

    init {
        viewModelScope.launch {
            clientId.collectLatest { value ->
                if (clientIdInput.value != value) {
                    clientIdInput.value = value
                }
            }
        }
        viewModelScope.launch {
            clientIdInput
                .debounce(400)
                .distinctUntilChanged()
                .collectLatest { value ->
                    walletMain.settingsRepository.set(clientId = value)
                }
        }
        viewModelScope.launch {
            walletMain.settingsRepository.openId4VpAllowedOriginSchemes.collectLatest { value ->
                val input = value.toInputString()
                if (openId4VpAllowedOriginSchemesInput.value != input) {
                    openId4VpAllowedOriginSchemesInput.value = input
                }
            }
        }
        viewModelScope.launch {
            openId4VpAllowedOriginSchemesInput
                .debounce(400)
                .distinctUntilChanged()
                .collectLatest { value ->
                    walletMain.settingsRepository.set(
                        openId4VpAllowedOriginSchemes = value.toOriginSchemeSet()
                    )
                }
        }
    }

    fun updateClientIdInput(value: String) {
        clientIdInput.update { value }
    }

    fun resetClientIdToDefault() {
        clientIdInput.value = SettingsRepository.DEFAULT_CLIENT_ID
    }

    fun updateOpenId4VpAllowedOriginSchemesInput(value: String) {
        openId4VpAllowedOriginSchemesInput.value = value
    }

    fun resetOpenId4VpAllowedOriginSchemesToDefault() {
        openId4VpAllowedOriginSchemesInput.value = defaultOpenId4VpAllowedOriginSchemes.toInputString()
    }

    fun showGlobalSnackbar(
        message: suspend () -> String,
    ) = walletMain.scope.launch {
        walletMain.snackbarService.showSnackbar(message())
    }

    fun onClickResetApp(
        completionHandler: CompletionHandler = {},
    ) = walletMain.scope.launch {
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
    ) = walletMain.scope.launch {
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

private fun Set<String>.toInputString(): String = sorted().joinToString(",")

private fun String.toOriginSchemeSet(): Set<String> =
    filterNot { it.isWhitespace() }
        .split(',', '\n')
        .map { it.trimEnd(':').lowercase() }
        .filter { it.isNotEmpty() }
        .toSet()
