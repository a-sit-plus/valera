package ui.views

import androidx.lifecycle.ViewModel
import at.asitplus.wallet.app.common.data.SettingsRepository

data class OnboardingViewModel(
    val settingsRepository: SettingsRepository,
) : ViewModel() {
    fun acceptConditions() {
        settingsRepository.set(isConditionsAccepted = true)
    }
}