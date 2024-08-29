package ui.navigation

import android.os.Parcel
import android.os.Parcelable
import data.verifier.Verifier
import data.storage.StoreEntryId
import kotlinx.parcelize.Parcelize
import org.intellij.lang.annotations.Pattern

@Parcelize
actual class HomePage : Page, Parcelable

@Parcelize
actual class LogPage : Page, Parcelable

@Parcelize
actual class SettingsPage : Page, Parcelable


@Parcelize
actual class OnboardingStartPage : OnboardingPage, Parcelable

@Parcelize
actual class OnboardingInformationPage : OnboardingPage, Parcelable

@Parcelize
actual class OnboardingTermsPage : OnboardingPage, Parcelable

@Parcelize
actual class AuthenticationLoadingPage : Page, Parcelable

@Parcelize
actual class ProvisioningLoadingPage actual constructor(
    actual val link: String
) : Page, Parcelable

@Parcelize
actual class AuthenticationQrCodeScannerPage : Page, Parcelable

@Parcelize
actual class RefreshRequirements actual constructor(
    actual val authenticationRequestParametersStringified: String,
) : Parcelable

@Parcelize
actual class RefreshCredentialsPage actual constructor(
    actual val refreshRequirements: RefreshRequirements?
) : Page, Parcelable

@Parcelize
actual class AddCredentialPage actual constructor() : Page, Parcelable

@Parcelize
actual class CredentialDetailsPage actual constructor(
    actual val storeEntryId: StoreEntryId
) : Page, Parcelable

@Parcelize
actual class AuthenticationConsentPage actual constructor(
    actual val authenticationRequestParametersFromSerialized: String,
    actual val authorizationPreparationStateSerialized: String,
    actual val recipientName: String,
    actual val recipientLocation: String,
    actual val fromQrCodeScanner: Boolean,
) : Page, Parcelable

@Parcelize
actual class AuthenticationSuccessPage : Page, Parcelable

@Parcelize
actual class SelectDataRetrievalPage : Page, Parcelable

@Parcelize
actual class CustomDataRetrievalPage : Page, Parcelable

actual class QrDeviceEngagementPage actual constructor(
    actual val document: Verifier.Document
) : Page

actual class LoadRequestedDataPage actual constructor(
    actual val document: Verifier.Document,
    actual val payload: String
): Page

@Parcelize
actual class RequestedDataLogOutputPage : Page, Parcelable

@Parcelize
actual class RequestedDataShowPage: Page, Parcelable
