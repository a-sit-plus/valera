package navigation

actual class HomePage actual constructor() : Page

actual class AboutPage actual constructor() : Page

actual class LogPage actual constructor() : Page

actual class CredentialPage actual constructor(actual val info: String) : Page

actual class CameraPage actual constructor() : Page
actual class PayloadPage actual constructor(actual val info: String) : Page
actual class AppLinkPage actual constructor() : Page

actual class LoadingPage actual constructor() : Page


actual class ConsentPage actual constructor(actual val url: String, actual val claims: List<String>, actual val recipientName: String, actual val recipientLocation: String): Page


actual class OnboardingStartPage actual constructor() : OnboardingPage

actual class OnboardingInformationPage actual constructor() : OnboardingPage

actual class OnboardingTermsPage actual constructor() : OnboardingPage
