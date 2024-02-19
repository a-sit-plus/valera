package navigation

actual class HomePage : Page

actual class AboutPage : Page

actual class LogPage : Page

actual class CredentialPage actual constructor(actual val info: String) : Page

actual class CameraPage : Page

actual class PayloadPage actual constructor(actual val info: String) : Page

actual class ConsentPage actual constructor(actual val url: String, actual val claims: List<String>, actual val recipientName: String, actual val recipientLocation: String) : Page



actual class AppLinkPage actual constructor() : Page

actual class OnboardingStartPage actual constructor() : OnboardingPage

actual class OnboardingInformationPage actual constructor() : OnboardingPage

actual class OnboardingTermsPage actual constructor() : OnboardingPage

actual class LoadingPage : Page