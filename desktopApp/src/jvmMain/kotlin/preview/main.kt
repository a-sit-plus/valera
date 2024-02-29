package preview

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.data.ConstantIndex
import navigation.AuthenticationConsentPage
import navigation.AuthenticationQrCodeScannerPage
import navigation.AuthenticationSuccessPage
import navigation.HomePage
import navigation.NavigationStack
import navigation.Page
import navigation.SettingsPage
import navigation.ShowDataPage
import ui.views.AuthenticationConsentView
import ui.views.MyDataView
import view.AuthenticationQrCodeScannerView
import view.AuthenticationSuccessScreen
import view.SettingsView
import view.ShowDataScreen

private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        PreviewNavigationScreen()
    }
}

private enum class Route(
    val title: String,
    val icon: @Composable () -> Unit,
    val destination: Page,
    val isActive: (Page) -> Boolean
) {
    MY_DATA_SCREEN(
        title = "Meine Daten",
        icon = {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Meine Daten ansehen",
            )
        },
        destination = HomePage(),
        isActive = {
            when (it) {
                is HomePage -> true
                else -> false
            }
        },
    ),
    SHOW_DATA_SCREEN(
        title = "Daten Vorzeigen",
        icon = {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = "Daten Vorzeigen",
            )
        },
        destination = ShowDataPage(),
        isActive = {
            when (it) {
                is ShowDataPage -> true
                else -> false
            }
        },
    ),
    INFORMATION_SCREEN(
        title = "Informationen",
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Weitere Information",
            )
        },
        destination = SettingsPage(),
        isActive = {
            when (it) {
                is SettingsPage -> true
                else -> false
            }
        },
    ),
}

@Composable
fun PreviewNavigationScreen() {
    val navigationData = { page: Page ->
        when (page) {
            is HomePage -> {
                Route.MY_DATA_SCREEN
            }

            is ShowDataPage -> {
                Route.SHOW_DATA_SCREEN
            }

            is SettingsPage -> {
                Route.INFORMATION_SCREEN
            }

            else -> null
        }
    }

    val navigationStack = rememberSaveable(
        saver = listSaver<NavigationStack<Page>, Page>(
            restore = { NavigationStack(*it.toTypedArray()) },
            save = { it.stack },
        )
    ) {
        NavigationStack(HomePage())
    }
    val globalBack = {
        navigationStack.back()
    }


    Scaffold(
        bottomBar = {
            val (_, page) = navigationStack.lastWithIndex()
            val pageNavigationData = navigationData(page)
            if (pageNavigationData != null) {
                NavigationBar {
                    for (route in listOf(
                        Route.MY_DATA_SCREEN,
                        Route.SHOW_DATA_SCREEN,
                        Route.INFORMATION_SCREEN,
                    )) {
                        NavigationBarItem(
                            icon = route.icon,
                            label = {
                                Text(route.title)
                            },
                            onClick = {
                                navigationStack.push(route.destination)
                            },
                            selected = route.isActive(page)
                        )
                    }
                }
            }
        },
    ) {
        Box(modifier = Modifier.padding(it)) {
            AnimatedContent(targetState = navigationStack.lastWithIndex()) { (_, page) ->
                when (page) {
                    is HomePage -> {
                        MyDataView(
                            refreshCredentials = {}
                        )
                    }

                    is ShowDataPage -> {
                        ShowDataScreen(
                            navigateToAuthenticationStartPage = {
                                navigationStack.push(
                                    AuthenticationConsentPage(
                                        url = "",
                                        recipientName = "Post-Schalter#3",
                                        recipientLocation = "St. Peter Hauptstraße\n8010, Graz",
                                        claims = listOf(
                                            IdAustriaScheme.Attributes.FIRSTNAME,
                                            IdAustriaScheme.Attributes.LASTNAME,
                                            IdAustriaScheme.Attributes.DATE_OF_BIRTH,
                                        ),
                                    )
                                )
                            },
                            onClickShowDataToExecutive = {},
                            onClickShowDataToOtherCitizen = {},
                        )
                    }

                    is SettingsPage -> {
                        SettingsView(
                            host = "http://www.example.com",
                            onChangeHost = {},
                            credentialRepresentation = ConstantIndex.CredentialRepresentation.PLAIN_JWT,
                            onChangeCredentialRepresentation = {},
                            isSaveEnabled = false,
                            onChangeIsSaveEnabled = {},
                            onClickSaveConfiguration = {},
                            stage = "T",
                            version = "1.0.0 / 2389237",
                            onClickFAQs = {},
                            onClickDataProtectionPolicy = {},
                            onClickLicenses = {},
                            onClickShareLogFile = {},
                            onClickResetApp = {},
                        )
                    }

                    is AuthenticationQrCodeScannerPage -> {
                        AuthenticationQrCodeScannerView(
                            navigateUp = globalBack,
                            onFoundPayload = { payload ->
                                navigationStack.push(
                                    AuthenticationConsentPage(
                                        url = "",
                                        recipientName = "spNameValue",
                                        recipientLocation = "spLocationValue",
                                        claims = listOf(),
                                    )
                                )
                            },
                        )
                    }

                    is AuthenticationSuccessPage -> {
                        AuthenticationSuccessScreen(navigateUp = globalBack)
                    }

                    is AuthenticationConsentPage -> {
                        AuthenticationConsentView(
                            navigateUp = globalBack,
                            cancelAuthentication = globalBack,
                            consentToDataTransmission = {
                                navigationStack.push(AuthenticationSuccessPage())
                            },
                            loadMissingData = {
                                globalBack()
                            },
                            requestedAttributes = listOf(),
                            spName = page.recipientName,
                            spLocation = page.recipientLocation,
                            spImage = null,
                            showBiometry = false,
                            onBiometryDismissed = {},
                            onBiometrySuccess = {},
                        )
                    }
                }
            }
        }
    }
}