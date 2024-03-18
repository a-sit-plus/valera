@file:OptIn(ExperimentalResourceApi::class, ExperimentalResourceApi::class)

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.jws.DefaultVerifierJwsService
import composewalletapp.shared.generated.resources.NAVIGATION_BUTTON_LABEL_MY_DATA
import composewalletapp.shared.generated.resources.NAVIGATION_BUTTON_LABEL_SETTINGS
import composewalletapp.shared.generated.resources.NAVIGATION_BUTTON_LABEL_SHOW_DATA
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.SNACKBAR_RESET_APP_SUCCESSFULLY
import domain.BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase
import domain.ExtractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase
import domain.RetrieveFinalAuthenticationRequestUriFromAuthenticationRequestUriUseCase
import io.github.aakira.napier.Napier
import io.ktor.http.parseQueryString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import ui.navigation.AuthenticationConsentPage
import ui.navigation.AuthenticationLoadingPage
import ui.navigation.AuthenticationQrCodeScannerPage
import ui.navigation.AuthenticationSuccessPage
import ui.navigation.HomePage
import ui.navigation.LogPage
import ui.navigation.NavigationStack
import ui.navigation.Page
import ui.navigation.ProvisioningLoadingPage
import ui.navigation.RefreshCredentialsPage
import ui.navigation.SettingsPage
import ui.screens.AuthenticationConsentScreen
import ui.screens.AuthenticationQrCodeScannerScreen
import ui.screens.AuthenticationQrCodeScannerViewModel
import ui.screens.AuthenticationSuccessScreen
import ui.screens.ErrorScreen
import ui.screens.LoadDataScreen
import ui.screens.LoadingScreen
import ui.screens.LogScreen
import ui.screens.MyCredentialsScreen
import ui.screens.OnboardingWrapper
import ui.screens.ProvisioningLoadingScreen
import ui.screens.SettingsScreen

private enum class NavigationData(
    val title: StringResource,
    val icon: @Composable () -> Unit,
    val destination: Page,
    val isActive: (Page) -> Boolean
) {
    HOME_SCREEN(
        title = Res.string.NAVIGATION_BUTTON_LABEL_MY_DATA,
        icon = {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
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
    AUTHENTICATION_SCANNING_SCREEN(
        title = Res.string.NAVIGATION_BUTTON_LABEL_SHOW_DATA,
        icon = {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = null,
            )
        },
        destination = AuthenticationQrCodeScannerPage(),
        isActive = {
            when (it) {
                is AuthenticationQrCodeScannerPage -> true
                else -> false
            }
        },
    ),
    INFORMATION_SCREEN(
        title = Res.string.NAVIGATION_BUTTON_LABEL_SETTINGS,
        icon = {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
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
fun Navigator(walletMain: WalletMain) {
    // Modified from https://github.com/JetBrains/compose-multiplatform/tree/master/examples/imageviewer
    val mainNavigationStack = rememberSaveable(
        saver = listSaver<NavigationStack<Page>, Page>(
            restore = { NavigationStack(*it.toTypedArray()) },
            save = { it.stack },
        )
    ) {
        NavigationStack(HomePage())
    }

    LaunchedEffect(appLink.value) {
        Napier.d("app link changed to ${appLink.value}")
        appLink.value?.let { link ->
            // resetting error service so that the intent can be displayed as intended
            walletMain.errorService.reset()

            Napier.d("new app link: ${link}")
            val parameterIndex = link.indexOfFirst { it == '?' }
            val pars = parseQueryString(link, startIndex = parameterIndex + 1)

            if (pars.contains("error")) {
                runBlocking {
                    walletMain.errorService.emit(
                        Exception(
                            pars["error_description"] ?: getString(
                                Res.string.UNKNOWN_EXCEPTION
                            )
                        )
                    )
                }
                appLink.value = null
                return@LaunchedEffect
            }

            val host = walletMain.walletConfig.host.first()
            if (link.contains("$host/mobile")) {
                Napier.d("authentication request")
                val extractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase =
                    ExtractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase(
                        verifierJwsService = DefaultVerifierJwsService(),
                    )

                val buildAuthenticationConsentPage =
                    BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(
                        extractAuthenticationRequestParametersFromAuthenticationRequestUri = extractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase,
                        retrieveFinalAuthenticationRequestUriFromAuthenticationRequestUriUseCase = RetrieveFinalAuthenticationRequestUriFromAuthenticationRequestUriUseCase(
                            client = walletMain.httpService.buildHttpClient(),
                            extractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase = extractAuthenticationRequestParametersFromAuthenticationRequestUriUseCase
                        )
                    )

                withContext(Dispatchers.IO) {
                    val authenticationConsentPage = buildAuthenticationConsentPage(link)
                    mainNavigationStack.push(authenticationConsentPage)
                }
                appLink.value = null
                return@LaunchedEffect
            }

            if (walletMain.provisioningService.redirectUri?.let { link.contains(it) } == true) {
                mainNavigationStack.push(
                    ProvisioningLoadingPage(
                        link = link
                    )
                )
                appLink.value = null
                return@LaunchedEffect
            }
        }
    }

    globalBack = { mainNavigationStack.back() }

    if (walletMain.errorService.showError.value == false) {
        OnboardingWrapper(
            walletMain = walletMain,
        ) {
            MainNavigator(
                navigationStack = mainNavigationStack,
                navigateUp = globalBack,
                walletMain = walletMain,
            )
        }
    } else {
        ErrorScreen(walletMain)
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun MainNavigator(
    navigationStack: NavigationStack<Page>,
    navigateUp: () -> Unit,
    walletMain: WalletMain,
) {
    Scaffold(
        bottomBar = {
            val (_, page) = navigationStack.lastWithIndex()
            val pageNavigationData = when (page) {
                is HomePage -> NavigationData.HOME_SCREEN

                is SettingsPage -> NavigationData.INFORMATION_SCREEN

                else -> null
            }

            if (pageNavigationData != null) {
                NavigationBar {
                    for (route in listOf(
                        NavigationData.HOME_SCREEN,
                        NavigationData.AUTHENTICATION_SCANNING_SCREEN,
                        NavigationData.INFORMATION_SCREEN,
                    )) {
                        NavigationBarItem(
                            icon = route.icon,
                            label = {
                                Text(stringResource(route.title))
                            },
                            onClick = {
                                if (route.isActive(page) == false) {
                                    navigationStack.push(route.destination)
                                }
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
                        MyCredentialsScreen(
                            navigateToRefreshCredentialsPage = {
                                navigationStack.push(RefreshCredentialsPage())
                            },
                            walletMain = walletMain,
                        )
                    }

                    is RefreshCredentialsPage -> {
                        LoadDataScreen(
                            navigateUp = navigateUp,
                            walletMain = walletMain,
                        )
                    }

                    is ProvisioningLoadingPage -> {
                        ProvisioningLoadingScreen(
                            link = page.link,
                            navigateUp = globalBack,
                            walletMain = walletMain,
                        )
                    }


                    is SettingsPage -> {
                        SettingsScreen(
                            navigateToLogPage = {
                                navigationStack.push(LogPage())
                            },
                            onClickResetApp = {
                                val resetMessage = runBlocking {
                                    walletMain.resetApp()
                                    getString(Res.string.SNACKBAR_RESET_APP_SUCCESSFULLY)
                                }
                                walletMain.snackbarService.showSnackbar(resetMessage)
                                navigationStack.reset()
                            },
                            walletMain = walletMain,
                        )
                    }

                    is LogPage -> {
                        LogScreen(
                            navigateUp = navigateUp,
                            walletMain = walletMain,
                        )
                    }


                    is AuthenticationQrCodeScannerPage -> {
                        AuthenticationQrCodeScannerScreen(
                            navigateUp = navigateUp,
                            navigateToConsentScreen = navigationStack::push,
                            navigateToLoadingScreen = {
                                navigationStack.push(AuthenticationLoadingPage())
                            },
                            authenticationQrCodeScannerViewModel = AuthenticationQrCodeScannerViewModel(
                                client = walletMain.httpService.buildHttpClient(),
                                verifierJwsService = DefaultVerifierJwsService(),
                            ),
                            walletMain = walletMain,
                        )
                    }

                    is AuthenticationLoadingPage -> {
                        LoadingScreen()
                    }

                    is AuthenticationConsentPage -> {
                        AuthenticationConsentScreen(
                            spName = page.recipientName,
                            spLocation = page.recipientLocation,
                            spImage = null,
                            claims = page.claims,
                            url = page.url,
                            fromQrCodeScanner = page.fromQrCodeScanner,
                            navigateUp = navigateUp,
                            navigateToRefreshCredentialsPage = {
                                navigationStack.push(
                                    RefreshCredentialsPage()
                                )
                            },
                            navigateToAuthenticationSuccessPage = {
                                navigationStack.push(
                                    AuthenticationSuccessPage()
                                )
                            },
                            walletMain = walletMain,
                        )
                    }

                    is AuthenticationSuccessPage -> {
                        AuthenticationSuccessScreen(
                            navigateUp = navigateUp,
                        )
                    }
                }
            }
        }
    }
}