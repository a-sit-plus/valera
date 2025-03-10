package ui.navigation.routes

import kotlinx.serialization.Serializable

internal object OnboardingWrapperTestTags {
    const val onboardingLoadingIndicator = "onboardingWrapperLoading"
    const val onboardingStartScreen = "onboardingStartScreen"
}

@Serializable
object OnboardingStartRoute : Route()

@Serializable
object OnboardingInformationRoute : Route()

@Serializable
object OnboardingTermsRoute : Route()