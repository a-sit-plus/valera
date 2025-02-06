package ui.navigation

import data.bletransfer.Holder
import data.bletransfer.Verifier

import data.storage.StoreEntryId

// TODO: cleanup (remove unused pages etc.)

actual class HomePage : Page

actual class LogPage : Page

actual class SettingsPage : Page


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

actual class CredentialDetailsPage actual constructor(
    actual val storeEntryId: StoreEntryId
) : Page

actual class AuthenticationConsentPage actual constructor(
    actual val authenticationRequestParametersFromSerialized: String,
    actual val authorizationPreparationStateSerialized: String,
    actual val recipientName: String,
    actual val recipientLocation: String,
    actual val fromQrCodeScanner: Boolean,
) : Page

actual class AuthenticationSuccessPage : Page

actual class SelectDataRetrievalPage : Page

actual class CustomDataRetrievalPage : Page

actual class QrDeviceEngagementPage actual constructor(actual val document: Verifier.Document) : Page

actual class LoadRequestedDataPage actual constructor(
    actual val document: Verifier.Document,
    actual val payload: String
): Page

actual class RequestedDataLogOutputPage : Page

actual class RequestedDataShowPage: Page

actual class ShowQrCodePage: Page

actual class HandleRequestedDataPage actual constructor(
    actual val holder: Holder
): Page
