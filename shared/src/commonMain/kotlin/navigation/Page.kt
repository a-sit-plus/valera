package navigation

interface Page

expect class HomePage() : Page

expect class AboutPage() : Page

expect class LogPage() : Page

expect class CredentialPage(info: String) : Page {
    val info: String
}

expect class CameraPage() : Page
expect class PayloadPage(info: String) : Page {
    val info: String
}
expect class AppLinkPage() : Page

expect class InformationPage() : Page


interface OnboardingPage

expect class OnboardingStartPage() : OnboardingPage

expect class OnboardingInformationPage() : OnboardingPage

expect class OnboardingTermsPage() : OnboardingPage
expect class ConsentPage(url: String, claims: List<String>, recipientName: String, recipientLocation: String): Page {
    val url: String
    val claims: List<String>
    val recipientName: String
    val recipientLocation: String
}

expect class LoadingPage(): Page

expect class RefreshCredentialsPage(): Page

expect class AuthenticationQrCodeScannerPage() : Page


expect class ShowDataPage() : Page

expect class QrCodeCredentialScannerPage() : Page