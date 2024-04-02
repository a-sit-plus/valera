package ui.navigation

actual class HomePage actual constructor() : Page

actual class LogPage actual constructor() : Page

actual class SettingsPage : Page


actual class AuthenticationLoadingPage actual constructor() : Page

actual class ProvisioningLoadingPage actual constructor(
    actual val link: String
) : Page

actual class AuthenticationQrCodeScannerPage actual constructor() : Page

actual class RefreshRequirements actual constructor(
    actual val requestedCredentialFormatHolderStringified: String,
    actual val requestedCredentialSchemeIdentifier: String,
    actual val requestedAttributes: Set<String>,
)

actual class RefreshCredentialsPage actual constructor(
    actual val refreshRequirements: RefreshRequirements?,
) : Page

actual class AuthenticationConsentPage actual constructor(
    actual val authenticationRequestParametersSerialized: String,
    actual val recipientName: String,
    actual val recipientLocation: String,
    actual val fromQrCodeScanner: Boolean,
) : Page

actual class AuthenticationSuccessPage : Page


actual class OnboardingStartPage actual constructor() : OnboardingPage

actual class OnboardingInformationPage actual constructor() : OnboardingPage

actual class OnboardingTermsPage actual constructor() : OnboardingPage