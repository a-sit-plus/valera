package ui.navigation

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

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
actual class RefreshCredentialsPage : Page, Parcelable

@Parcelize
actual class AuthenticationConsentPage actual constructor(
    actual val authenticationRequestParametersSerialized: String,
    actual val recipientName: String,
    actual val recipientLocation: String,
    actual val fromQrCodeScanner: Boolean,
) : Page, Parcelable

@Parcelize
actual class AuthenticationSuccessPage : Page, Parcelable