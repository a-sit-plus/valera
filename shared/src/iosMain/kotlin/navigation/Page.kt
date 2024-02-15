package navigation

actual class HomePage : Page

actual class AboutPage : Page

actual class CredentialPage actual constructor(actual val info: String) : Page

actual class CameraPage : Page

actual class PayloadPage actual constructor(actual val info: String) : Page

actual class AppLinkPage : Page


actual class OnboardingStartPage actual constructor() : OnboardingPage

actual class OnboardingInformationPage actual constructor() : OnboardingPage

actual class OnboardingTermsPage actual constructor() : OnboardingPage