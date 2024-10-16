package ui.navigation

import data.storage.StoreEntryId

actual class HomePage : Page

actual class LogPage : Page

actual class PreAuthQrCodeScannerPage : Page

actual class SettingsPage : Page

actual class LoadingPage : Page


actual class OnboardingStartPage actual constructor() : OnboardingPage

actual class OnboardingInformationPage actual constructor() : OnboardingPage

actual class OnboardingTermsPage actual constructor() : OnboardingPage

actual class AuthenticationLoadingPage : Page

actual class ProvisioningLoadingPage actual constructor(
    actual val link: String
) : Page

actual class AuthenticationQrCodeScannerPage : Page

actual class RefreshRequirements actual constructor(
    actual val authenticationRequestParametersStringified: String,
)

actual class RefreshCredentialsPage actual constructor(
    actual val refreshRequirements: RefreshRequirements?,
) : Page
actual class AddCredentialPage : Page
actual class AddCredentialPreAuthnPage actual constructor(
    actual val credentialOfferInfoSerialized: String,
): Page

actual class CredentialDetailsPage actual constructor(
    actual val storeEntryId: StoreEntryId
) : Page

actual class AuthenticationConsentPage actual constructor(
    actual val authenticationRequestParametersFromSerialized: String,
    actual val authorizationPreparationStateSerialized: String,
    actual val recipientName: String,
    actual val recipientLocation: String,
) : Page

actual class AuthenticationSuccessPage : Page