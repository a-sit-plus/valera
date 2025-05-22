package ui.viewmodels

import at.asitplus.wallet.app.data.SettingsRepository

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : SettingsRepository by settingsRepository