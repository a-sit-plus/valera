package navigation

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
actual class HomePage : Page, Parcelable

@Parcelize
actual class AboutPage : Page, Parcelable

@Parcelize
actual class LogPage : Page, Parcelable

@Parcelize
actual class CredentialPage actual constructor(actual val info: String) : Page, Parcelable

@Parcelize
actual class CameraPage : Page, Parcelable

@Parcelize
actual class PayloadPage actual constructor(actual val info: String) : Page, Parcelable

@Parcelize
actual class ConsentPage actual constructor(
    actual val url: String,
    actual val claims: List<String>,
    actual val recipientName: String,
    actual val recipientLocation: String,
) : Page, Parcelable


@Parcelize
actual class AppLinkPage : Page, Parcelable

@Parcelize
actual class InformationPage : Page, Parcelable


@Parcelize
actual class OnboardingStartPage actual constructor() : OnboardingPage, Parcelable

@Parcelize
actual class OnboardingInformationPage actual constructor() : OnboardingPage, Parcelable

@Parcelize
actual class OnboardingTermsPage actual constructor() : OnboardingPage, Parcelable

@Parcelize
actual class LoadingPage : Page, Parcelable

@Parcelize
actual class AuthenticationQrCodeScannerPage : Page, Parcelable

@Parcelize
actual class ShowDataPage : Page, Parcelable

@Parcelize
actual class RefreshCredentialsPage : Page, Parcelable

@Parcelize
actual class QrCodeCredentialScannerPage : Page, Parcelable