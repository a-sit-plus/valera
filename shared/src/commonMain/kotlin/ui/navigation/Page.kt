package ui.navigation

interface Page

expect class HomePage() : Page

expect class LogPage() : Page

expect class SettingsPage() : Page


interface OnboardingPage

expect class OnboardingStartPage() : OnboardingPage

expect class OnboardingInformationPage() : OnboardingPage

expect class OnboardingTermsPage() : OnboardingPage

expect class ProvisioningLoadingPage(link: String) : Page {
    val link: String
}

expect class RefreshCredentialsPage() : Page



expect class AuthenticationQrCodeScannerPage() : Page

expect class AuthenticationLoadingPage() : Page

expect class AuthenticationConsentPage(
    authenticationRequestParametersSerialized: String,
    recipientName: String,
    recipientLocation: String,
    fromQrCodeScanner: Boolean = false
) : Page {
    val authenticationRequestParametersSerialized: String
    val recipientName: String
    val recipientLocation: String
    val fromQrCodeScanner: Boolean
}

expect class AuthenticationSuccessPage() : Page