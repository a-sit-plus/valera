package ui.navigation

import data.bletransfer.Verifier
import data.storage.StoreEntryId

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
    recipientName: String,
    recipientLocation: String,
    fromQrCodeScanner: Boolean = false
) : Page {
    val authenticationRequestParametersFromSerialized: String
    val authorizationPreparationStateSerialized: String
    val recipientName: String
    val recipientLocation: String
    val fromQrCodeScanner: Boolean
}

expect class AuthenticationSuccessPage() : Page

expect class SelectDataRetrievalPage() : Page

expect class CustomDataRetrievalPage() : Page

expect class QrDeviceEngagementPage(
    document: Verifier.Document
) : Page {
    val document: Verifier.Document
}

expect class LoadRequestedDataPage(
    document: Verifier.Document,
    payload: String
): Page {
    val document: Verifier.Document
    val payload: String
}

expect class RequestedDataLogOutputPage() : Page

expect class RequestedDataShowPage() : Page

expect class ShowQrCodePage(): Page

