package ui.navigation

import data.storage.StoreEntryId

interface Page

expect class HomePage() : Page

expect class LogPage() : Page

expect class PreAuthQrCodeScannerPage() : Page

expect class SettingsPage() : Page

expect class LoadingPage() : Page


interface OnboardingPage

expect class OnboardingStartPage() : OnboardingPage

expect class OnboardingInformationPage() : OnboardingPage

expect class OnboardingTermsPage() : OnboardingPage

expect class ProvisioningLoadingPage(link: String) : Page {
    val link: String
}

expect class RefreshRequirements(
    authenticationRequestParametersStringified: String,
) {
    val authenticationRequestParametersStringified: String
}

expect class RefreshCredentialsPage(
    refreshRequirements: RefreshRequirements? = null,
) : Page {
    val refreshRequirements: RefreshRequirements?
}

expect class AddCredentialPage() : Page

expect class AddCredentialPreAuthnPage(
    credentialOfferInfoSerialized: String
) : Page {
    val credentialOfferInfoSerialized: String
}

expect class CredentialDetailsPage(
    storeEntryId: StoreEntryId
) : Page {
    val storeEntryId: StoreEntryId
}



expect class AuthenticationQrCodeScannerPage() : Page

expect class AuthenticationLoadingPage() : Page

expect class AuthenticationConsentPage(
    authenticationRequestParametersFromSerialized: String, // AuthenticationRequest
    authorizationPreparationStateSerialized: String, // AuthenticationResultParameters
    recipientLocation: String,
) : Page {
    val authenticationRequestParametersFromSerialized: String
    val authorizationPreparationStateSerialized: String
    val recipientLocation: String
}

expect class AuthenticationSuccessPage() : Page