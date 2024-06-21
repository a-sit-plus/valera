package ui.navigation

interface Page

interface HomePage : Page

interface LogPage : Page

interface SettingsPage : Page

interface ProvisioningLoadingPage : Page {
    val link: String
}

interface AddCredentialPage : Page


interface AuthenticationQrCodeScannerPage : Page

interface AuthenticationLoadingPage : Page

interface AuthenticationConsentPage : Page {
    val authenticationRequestSerialized: String
    val authenticationResponseSerialized: String
    val recipientName: String
    val recipientLocation: String
    val fromQrCodeScanner: Boolean

    companion object {
        fun interface Builder {
            operator fun invoke(
                authenticationRequestSerialized: String,
                authenticationResponseSerialized: String,
                recipientName: String,
                recipientLocation: String,
                fromQrCodeScanner: Boolean
            ): AuthenticationConsentPage
        }
    }
}

interface AuthenticationSuccessPage : Page


interface OnboardingPage

interface OnboardingStartPage : OnboardingPage

interface OnboardingInformationPage : OnboardingPage

interface OnboardingTermsPage : OnboardingPage


class NavigationPages(
    val homePage: () -> HomePage,
    val logPage: () -> LogPage,
    val settingsPage: () -> SettingsPage,
    val provisioningLoadingPage: (link: String) -> ProvisioningLoadingPage,
    val addCredentialPage: () -> AddCredentialPage,
    val authenticationQrCodeScannerPage: () -> AuthenticationQrCodeScannerPage,
    val authenticationLoadingPage: () -> AuthenticationLoadingPage,
    val authenticationConsentPage: AuthenticationConsentPage.Companion.Builder,
    val authenticationSuccessPage: () -> AuthenticationSuccessPage,

    val onboardingStartPage: () -> OnboardingStartPage,
    val onboardingInformationPage: () -> OnboardingInformationPage,
    val onboardingTermsPage: () -> OnboardingTermsPage,
) {
    companion object {
        fun createWithDefaults(
            homePageBuilder: (() -> HomePage)? = null,
            logPageBuilder: (() -> LogPage)? = null,
            settingsPageBuilder: (() -> SettingsPage)? = null,
            provisioningLoadingPageBuilder: ((link: String) -> ProvisioningLoadingPage)? = null,
            addCredentialPageBuilder: (() -> AddCredentialPage)? = null,
            authenticationQrCodeScannerPageBuilder: (() -> AuthenticationQrCodeScannerPage)? = null,
            authenticationLoadingPageBuilder: (() -> AuthenticationLoadingPage)? = null,
            authenticationConsentPageBuilder: AuthenticationConsentPage.Companion.Builder? = null,
            authenticationSuccessPageBuilder: (() -> AuthenticationSuccessPage)? = null,

            onboardingStartPageBuilder: (() -> OnboardingStartPage)? = null,
            onboardingInformationPageBuilder: (() -> OnboardingInformationPage)? = null,
            onboardingTermsPageBuilder: (() -> OnboardingTermsPage)? = null,
        ) = NavigationPages(
            homePage = homePageBuilder ?: { object : HomePage {} },
            logPage = logPageBuilder ?: { object : LogPage {} },
            settingsPage = settingsPageBuilder ?: { object : SettingsPage {} },
            provisioningLoadingPage = provisioningLoadingPageBuilder ?: { link ->
                object : ProvisioningLoadingPage {
                    override val link: String
                        get() = link
                }
            },
            addCredentialPage = addCredentialPageBuilder ?: {
                object : AddCredentialPage {}
            },
            authenticationQrCodeScannerPage = authenticationQrCodeScannerPageBuilder
                ?: { object : AuthenticationQrCodeScannerPage {} },
            authenticationLoadingPage = authenticationLoadingPageBuilder ?: {
                object : AuthenticationLoadingPage {}
            },
            authenticationConsentPage = authenticationConsentPageBuilder
                ?: AuthenticationConsentPage.Companion.Builder { authenticationRequestSerialized, authenticationResponseSerialized, recipientName, recipientLocation, fromQrCodeScanner ->
                    object : AuthenticationConsentPage {
                        override val authenticationRequestSerialized: String
                            get() = authenticationRequestSerialized
                        override val authenticationResponseSerialized: String
                            get() = authenticationResponseSerialized
                        override val recipientName: String
                            get() = recipientName
                        override val recipientLocation: String
                            get() = recipientLocation
                        override val fromQrCodeScanner: Boolean
                            get() = fromQrCodeScanner
                    }
                },
            authenticationSuccessPage = authenticationSuccessPageBuilder ?: {
                object : AuthenticationSuccessPage {}
            },
            onboardingStartPage = onboardingStartPageBuilder ?: {
                object : OnboardingStartPage {}
            },
            onboardingInformationPage = onboardingInformationPageBuilder ?: {
                object : OnboardingInformationPage {}
            },
            onboardingTermsPage = onboardingTermsPageBuilder ?: {
                object : OnboardingTermsPage {}
            },
        )
    }
}