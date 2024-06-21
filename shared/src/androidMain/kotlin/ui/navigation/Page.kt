package ui.navigation

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class AndroidHomePage : HomePage, Parcelable

@Parcelize
class AndroidLogPage : LogPage, Parcelable

@Parcelize
class AndroidSettingsPage : SettingsPage, Parcelable

@Parcelize
class AndroidAuthenticationLoadingPage : AuthenticationLoadingPage, Parcelable

@Parcelize
class AndroidProvisioningLoadingPage(
    override val link: String
) : ProvisioningLoadingPage, Parcelable

@Parcelize
class AndroidAuthenticationQrCodeScannerPage : AuthenticationQrCodeScannerPage, Parcelable


@Parcelize
class AndroidAddCredentialPage : AddCredentialPage, Parcelable

@Parcelize
class AndroidAuthenticationConsentPage(
    override val authenticationRequestSerialized: String,
    override val authenticationResponseSerialized: String,
    override val recipientName: String,
    override val recipientLocation: String,
    override val fromQrCodeScanner: Boolean,
) : AuthenticationConsentPage, Parcelable

@Parcelize
class AndroidAuthenticationSuccessPage : AuthenticationSuccessPage, Parcelable




@Parcelize
class AndroidOnboardingStartPage : OnboardingStartPage, Parcelable

@Parcelize
class AndroidOnboardingInformationPage : OnboardingInformationPage, Parcelable

@Parcelize
class AndroidOnboardingTermsPage : OnboardingTermsPage, Parcelable