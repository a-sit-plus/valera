import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import at.asitplus.wallet.app.common.SnackbarService
import at.asitplus.wallet.app.common.WalletMain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import navigation.AboutPage
import navigation.AppLinkPage
import navigation.CameraPage
import navigation.CredentialPage
import navigation.HomePage
import navigation.NavigationStack
import navigation.Page
import navigation.PayloadPage
import ui.composables.AttributeAvailability
import ui.composables.PersonalDataCategory
import ui.theme.WalletTheme
import ui.views.AuthenticationConsentPage
import ui.views.AuthenticationConsentView
import ui.views.AuthenticationQrCodeScannerPage
import ui.views.AuthenticationQrCodeScannerView
import ui.views.AuthenticationSPInfoPage
import ui.views.AuthenticationSPInfoView
import ui.views.InformationPage
import ui.views.InformationView
import ui.views.ShowDataPage
import ui.views.ShowDataView
import view.AboutScreen
import view.AppLinkScreen
import view.CameraView
import view.CredentialScreen
import view.HomeScreen
import view.MyDataScreen
import view.OnboardingWrapper
import view.PayloadScreen

//
///**
// * Global variable to utilize back navigation functionality
// */
//var globalBack: () -> Unit = {}
//
///**
// * Global variable which especially helps to channel information from swift code
// * to compose whenever the app gets called via an associated domain
// */
//var appLink = mutableStateOf<String?>(null)
//
//
///**
// * Global variable to test at least something from the iOS UITest
// */
//var iosTestValue = Resources.IOS_TEST_VALUE


private enum class Route(
    val title: String,
    val icon: @Composable () -> Unit,
    val destination: Page,
    val isActive: (Page) -> Boolean
) {
    HOME_SCREEN(
        title = "Meine Daten",
        icon = {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Meine Daten ansehen",
            )
        },
        destination = HomePage(),
        isActive = {
            when(it) {
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
            when(it) {
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
        destination = InformationPage(),
        isActive = {
            when(it) {
                is InformationPage -> true
                else -> false
            }
        },
    ),
}

@Composable
fun App(walletMain: WalletMain) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarService = SnackbarService(scope, snackbarHostState)

    try {
        walletMain.initialize(snackbarService)
    } catch (e: Exception) {
        walletMain.errorService.emit(e)
    }

    WalletTheme {
        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { _ ->
            if (walletMain.errorService.showError.value == false) {
                OnboardingWrapper(
                    walletMain = walletMain,
                ) {
                    navigator2(walletMain)
                }
            } else {
                errorScreen(walletMain)
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun navigator2(walletMain: WalletMain) {
    key(appLink.value) {
        val defaultPage: Page
        if (appLink.value == null) {
            defaultPage = HomePage()
        } else {
            defaultPage = AppLinkPage()
        }

        val navigationData = { page: Page ->
            when (page) {
                is HomePage -> {
                    Route.HOME_SCREEN
                }

                is ShowDataPage -> {
                    Route.SHOW_DATA_SCREEN
                }

                is InformationPage -> {
                    Route.INFORMATION_SCREEN
                }

                else -> null
            }
        }

        // Modified from https://github.com/JetBrains/compose-multiplatform/tree/master/examples/imageviewer
        val navigationStack = rememberSaveable(
            saver = listSaver(
                restore = { NavigationStack(*it.toTypedArray()) },
                save = { it.stack },
            )
        ) {
            NavigationStack(defaultPage)
        }

        globalBack = { navigationStack.back() }

        Scaffold(
            bottomBar = {
                val (_, page) = navigationStack.lastWithIndex()
                val pageNavigationData = navigationData(page)
                if (pageNavigationData != null) {
                    NavigationBar {
                        for (route in listOf(
                            Route.HOME_SCREEN,
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
        ) { scaffoldPadding ->
            Box(modifier = Modifier.padding(scaffoldPadding)) {
                AnimatedContent(targetState = navigationStack.lastWithIndex()) { (_, page) ->
                    when (page) {
                        is HomePage -> {
                            MyDataScreen(
                                walletMain = walletMain,
                                refreshCredentials = {
                                    CoroutineScope(Dispatchers.Default).launch {
                                        try {
                                            walletMain.provisioningService.startProvisioning()
                                        } catch (e: Exception) {
                                            walletMain.errorService.emit(e)
                                        }
                                    }
                                }
                            )
//                        HomeScreen(
//                            onAbout = { navigationStack.push(AboutPage()) },
//                            onCredential = { info ->
//                                navigationStack.push(CredentialPage(info))
//                            },
//                            onScanQrCode = { navigationStack.push(CameraPage()) },
//                            onLoginWithIdAustria = {
//                                CoroutineScope(Dispatchers.Default).launch {
//                                    try {
//                                        walletMain.provisioningService.startProvisioning()
//                                    } catch (e: Exception) {
//                                        walletMain.errorService.emit(e)
//                                    }
//                                }
//                            },
//                            walletMain = walletMain
//                        )
                        }

                        is AboutPage -> {
                            AboutScreen(walletMain)
                        }

                        is CredentialPage -> {
                            CredentialScreen(id = page.info, walletMain)
                        }

                        is CameraPage -> {
                            CameraView(
                                onFoundPayload = { info ->
                                    navigationStack.push(PayloadPage(info))
                                }
                            )
                        }

                        is PayloadPage -> {
                            PayloadScreen(
                                text = page.info,
                                onContinueClick = { navigationStack.push(HomePage()) },
                                walletMain
                            )

                        }

                        is AppLinkPage -> {
                            AppLinkScreen(
                                walletMain = walletMain
                            )
                        }


                        is ShowDataPage -> {
                            ShowDataView(
                                navigateToAuthenticationAtSp = {
//                                navigationStack.push(AuthenticationQrCodeScannerPage())
                                    navigationStack.push(
                                        AuthenticationConsentPage(
                                            spName = "Post-Schalter#3",
                                            spLocation = "St. Peter Hauptstraße\n8010, Graz",
                                            requestedAttributes = mapOf(
                                                PersonalDataCategory.IdentityData to listOf(
                                                    AttributeAvailability(
                                                        attributeName = "Vorname",
                                                        isAvailable = false,
                                                    ),
                                                    AttributeAvailability(
                                                        attributeName = "Nachname",
                                                        isAvailable = false,
                                                    ),
                                                    AttributeAvailability(
                                                        attributeName = "Aktuelles Foto aus zentralem Identitätsdokumentenregister",
                                                        isAvailable = false,
                                                    ),
                                                ),
                                                PersonalDataCategory.ResidencyData to listOf(
                                                    AttributeAvailability(
                                                        attributeName = "Straße",
                                                        isAvailable = false,
                                                    ),
                                                    AttributeAvailability(
                                                        attributeName = "Hausnummer",
                                                        isAvailable = false,
                                                    ),
                                                    AttributeAvailability(
                                                        attributeName = "Postleitzahl",
                                                        isAvailable = true,
                                                    ),
                                                    AttributeAvailability(
                                                        attributeName = "Ort",
                                                        isAvailable = true,
                                                    ),
                                                ),
                                            ).toList()
                                        )
                                    )
                                },
                                navigateToShowDataToExecutive = {},
                                navigateToShowDataToOtherCitizen = {},
                            )
                        }

                        is InformationPage -> {
                            InformationView(
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
                                navigateToConsentScreenWithResult = { name, location ->
                                    navigationStack.push(
                                        AuthenticationSPInfoPage(
                                            spName = name,
                                            spLocation = location,
                                        )
                                    )
                                },
                            )
                        }

                        is AuthenticationSPInfoPage -> {
                            AuthenticationSPInfoView(
                                navigateUp = globalBack,
                                cancelAuthentication = {},
                                authenticateAtSp = {},
                                spName = page.spName,
                                spLocation = page.spLocation,
                            )
                        }

                        is AuthenticationConsentPage -> {
                            val bottomSheetState = rememberModalBottomSheetState()
                            var showBottomSheet by remember { mutableStateOf(false) }

                            AuthenticationConsentView(
                                navigateUp = globalBack,
                                cancelAuthentication = globalBack,
                                consentToDataTransmission = {
                                    showBottomSheet = true
                                },
                                loadMissingData = {
                                    globalBack()

                                    navigationStack.push(AuthenticationConsentPage(
                                        spName = page.spName,
                                        spLocation = page.spLocation,
                                        requestedAttributes = page.requestedAttributes.map {
                                            it.copy(second = it.second.map {
                                                it.copy(isAvailable = true)
                                            })
                                        }
                                    ))
                                },
                                requestedAttributes = page.requestedAttributes,
                                spName = page.spName,
                                spLocation = page.spLocation,
                                bottomSheetState = bottomSheetState,
                                showBottomSheet = showBottomSheet,
                                onBottomSheetDismissRequest = {
                                    showBottomSheet = !showBottomSheet
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}