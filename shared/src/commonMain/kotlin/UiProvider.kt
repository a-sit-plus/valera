import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import ui.composables.BiometryPrompt
import ui.composables.BiometryPromptSuccessResult
import ui.navigation.AddCredentialPage
import ui.navigation.AuthenticationConsentPage
import ui.navigation.AuthenticationLoadingPage
import ui.navigation.AuthenticationQrCodeScannerPage
import ui.navigation.AuthenticationSuccessPage
import ui.navigation.HomePage
import ui.navigation.LogPage
import ui.navigation.NavigationPages
import ui.navigation.OnboardingInformationPage
import ui.navigation.OnboardingStartPage
import ui.navigation.OnboardingTermsPage
import ui.navigation.ProvisioningLoadingPage
import ui.navigation.SettingsPage
import ui.theme.lightScheme
import ui.views.CameraView


data class UiProvider(
    val colorScheme: ColorScheme,
    val biometryPrompt: BiometryPrompt,
    val cameraView: CameraView,
    val navigationPages: NavigationPages,
)

val LocalUiProvider = compositionLocalOf {
    UiProvider(
        colorScheme = lightScheme,
        biometryPrompt = { title, subtitle, onSuccess, onDismiss ->
            LaunchedEffect(true) {
                onSuccess(BiometryPromptSuccessResult())
            }
        },
        cameraView = { onFoundPayload, modifier ->
            LaunchedEffect(true) {
                onFoundPayload("")
            }
        },
        navigationPages = NavigationPages(
            homePage = { object : HomePage {} },
            logPage = { object : LogPage {} },
            settingsPage = { object : SettingsPage {} },
            provisioningLoadingPage = { link ->
                object : ProvisioningLoadingPage {
                    override val link: String
                        get() = link
                }
            },
            addCredentialPage = { object : AddCredentialPage {} },
            authenticationQrCodeScannerPage = { object : AuthenticationQrCodeScannerPage {} },
            authenticationLoadingPage = { object : AuthenticationLoadingPage {} },
            authenticationConsentPage = { authenticationRequestSerialized, authenticationResponseSerialized, recipientName, recipientLocation, fromQrCodeScanner ->
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
            authenticationSuccessPage = { object : AuthenticationSuccessPage {} },
            onboardingStartPage = { object : OnboardingStartPage {} },
            onboardingInformationPage = { object : OnboardingInformationPage {} },
            onboardingTermsPage = { object : OnboardingTermsPage {} },
        )
    )
}