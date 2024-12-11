package ui.viewmodels

import at.asitplus.wallet.app.common.Configuration
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.error_feature_not_yet_available
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

class SettingsViewModel (walletMain: WalletMain,
                         val onClickShareLogFile: () -> Unit,
                         val onClickClearLogFile: () -> Unit,
                         val onClickResetApp: () -> Unit) {
    val stage = Configuration.BUILD_FOR_STAGE
    val buildType = walletMain.buildContext.buildType
    val version = walletMain.buildContext.versionName

    val onClickFAQs = {
        runBlocking {
            walletMain.snackbarService.showSnackbar(getString(Res.string.error_feature_not_yet_available))
        }
    }
    val onClickDataProtectionPolicy = {
        runBlocking {
            walletMain.snackbarService.showSnackbar(getString(Res.string.error_feature_not_yet_available))
        }
    }
    val onClickLicenses = {
        runBlocking {
            walletMain.snackbarService.showSnackbar(getString(Res.string.error_feature_not_yet_available))
        }
    }
}