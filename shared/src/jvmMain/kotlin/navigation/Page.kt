package navigation

actual class HomePage actual constructor() : Page

actual class AboutPage actual constructor() : Page

actual class CredentialPage actual constructor(actual val info: String) : Page

actual class CameraPage actual constructor() : Page
actual class PayloadPage actual constructor(actual val info: String) : Page
actual class AppLinkPage actual constructor() : Page

actual class OnboardingStartPage actual constructor() : OnboardingPage

actual class OnboardingInformationPage actual constructor() : OnboardingPage

actual class OnboardingTermsPage actual constructor() : OnboardingPage