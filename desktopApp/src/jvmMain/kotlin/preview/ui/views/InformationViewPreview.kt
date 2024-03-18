package preview.ui.views

import at.asitplus.wallet.app.common.BuildEnvironment
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import at.asitplus.wallet.lib.data.ConstantIndex
import ui.screens.SettingsView

private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        SettingsView(
            host = "http://www.example.com",
            onChangeHost = {},
            credentialRepresentation = ConstantIndex.CredentialRepresentation.PLAIN_JWT,
            onChangeCredentialRepresentation = {},
            isSaveEnabled = false,
            onClickSaveConfiguration = {},
            buildType = "T",
            version = "1.0.0 / 2389237",
            onClickFAQs = {},
            onClickDataProtectionPolicy = {},
            onClickLicenses = {},
            onClickShareLogFile = {},
            onClickResetApp = {},
            stage = BuildEnvironment.Development.abbreviation,
        )
    }
}
