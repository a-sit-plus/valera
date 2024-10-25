package ui.navigation.Routes

import kotlinx.serialization.Serializable

internal object OnboardingWrapperTestTags {
    const val onboardingLoadingIndicator = "onboardingWrapperLoading"
    const val onboardingStartScreen = "onboardingStartScreen"
}

@Serializable
object OnboardingStart : Route()

@Serializable
object OnboardingInformation : Route()

@Serializable
object OnboardingTerms : Route()