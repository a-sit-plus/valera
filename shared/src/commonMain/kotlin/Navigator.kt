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
import androidx.compose.ui.platform.testTag
import at.asitplus.wallet.app.common.WalletMain
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.navigation_button_label_my_data
import composewalletapp.shared.generated.resources.navigation_button_label_settings
import composewalletapp.shared.generated.resources.navigation_button_label_show_data
import composewalletapp.shared.generated.resources.snackbar_reset_app_successfully
import data.vclib.AuthenticationRequest
import data.vclib.AuthenticationResponseResultSerializable
import domain.BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase
import io.github.aakira.napier.Napier
import io.ktor.http.parseQueryString
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import ui.navigation.AddCredentialPage
import ui.navigation.AuthenticationConsentPage
import ui.navigation.AuthenticationLoadingPage
import ui.navigation.AuthenticationQrCodeScannerPage
import ui.navigation.AuthenticationSuccessPage
import ui.navigation.HomePage
import ui.navigation.LogPage
import ui.navigation.NavigationStack
import ui.navigation.Page
import ui.navigation.ProvisioningLoadingPage
import ui.navigation.SettingsPage
import ui.screens.AddCredentialScreen
import ui.screens.AuthenticationConsentScreen
import ui.screens.AuthenticationQrCodeScannerScreen
import ui.screens.AuthenticationSuccessScreen
import ui.screens.ErrorScreen
import ui.screens.LoadingScreen
import ui.screens.LogScreen
import ui.screens.MyCredentialsScreen
import ui.screens.OnboardingWrapper
import ui.screens.ProvisioningLoadingScreen
import ui.screens.SettingsScreen
import view.AuthenticationQrCodeScannerViewModel

internal object NavigatorTestTags {
    const val postOnboardingContent = "postOnboardingContent"
}

@Composable
fun Navigator(walletMain: WalletMain) {
    val uiProvider = LocalUiProvider.current
    // Modified from https://github.com/JetBrains/compose-multiplatform/tree/master/examples/imageviewer
    val mainNavigationStack = rememberSaveable(
        saver = listSaver<NavigationStack<Page>, Page>(
            restore = { NavigationStack(*it.toTypedArray()) },
            save = { it.stack },
        )
    ) {
        NavigationStack(uiProvider.navigationPages.homePage())
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
                        Exception(pars["error_description"] ?: "Unknown Exception")
                    )
                }
                appLink.value = null
                return@LaunchedEffect
            }

            if (walletMain.provisioningService.redirectUri?.let { link.contains(it) } == true) {
                walletMain.provisioningService.redirectUri = null
                mainNavigationStack.push(
                    uiProvider.navigationPages.provisioningLoadingPage(link)
                )
                appLink.value = null
                return@LaunchedEffect
            }

            // if this is not for provisioning, it must be an authorization request
            kotlin.run {
                val consentPageBuilder =
                    BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(
                        oidcSiopWallet = walletMain.presentationService.oidcSiopWallet,
                        consentPageBuilder = uiProvider.navigationPages.authenticationConsentPage,
                        fromQrCodeScanner = false,
                    )

                consentPageBuilder(link).unwrap().onSuccess {
                    Napier.d("valid authentication request")
                    mainNavigationStack.push(it)
                }.onFailure {
                    Napier.d("invalid authentication request")
                }
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
                modifier = Modifier.testTag(NavigatorTestTags.postOnboardingContent),
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
    modifier: Modifier = Modifier,
) {
    val uiProvider = LocalUiProvider.current
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
                        val destination = route.destination()
                        NavigationBarItem(
                            icon = route.icon,
                            label = {
                                Text(stringResource(route.title))
                            },
                            onClick = {
                                if (route.isActive(page) == false) {
                                    navigationStack.push(destination)
                                }
                            },
                            selected = route.isActive(page),
                        )
                    }
                }
            }
        },
        modifier = modifier,
    ) { scaffoldPadding ->
        Box(modifier = Modifier.padding(scaffoldPadding)) {
            AnimatedContent(targetState = navigationStack.lastWithIndex()) { (_, page) ->
                when (page) {
                    is HomePage -> {
                        MyCredentialsScreen(
                            navigateToAddCredentialsPage = {
                                navigationStack.push(uiProvider.navigationPages.addCredentialPage())
                            },
                            walletMain = walletMain,
                        )
                    }

                    is AddCredentialPage -> {
                        AddCredentialScreen(
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
                                navigationStack.push(uiProvider.navigationPages.logPage())
                            },
                            onClickResetApp = {
                                val resetMessage = runBlocking {
                                    walletMain.resetApp()
                                    getString(Res.string.snackbar_reset_app_successfully)
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
                                navigationStack.push(uiProvider.navigationPages.authenticationLoadingPage())
                            },
                            authenticationQrCodeScannerViewModel = AuthenticationQrCodeScannerViewModel(
                                oidcSiopWallet = walletMain.presentationService.oidcSiopWallet,
                                consentPageBuilder = uiProvider.navigationPages.authenticationConsentPage,
                            ),
                            walletMain = walletMain,
                        )
                    }

                    is AuthenticationLoadingPage -> {
                        LoadingScreen()
                    }

                    is AuthenticationConsentPage -> {
                        kotlin.runCatching {
                            val request =
                                AuthenticationRequest.deserialize(page.authenticationRequestSerialized)
                                    .getOrThrow()
                            val result =
                                AuthenticationResponseResultSerializable.deserialize(page.authenticationResponseSerialized)
                                    .getOrThrow()

                            AuthenticationRequest.deserialize(page.authenticationRequestSerialized)
                                .unwrap().onFailure {
                                    LaunchedEffect(true) {
                                        navigateUp()
                                        walletMain.errorService.emit(it)
                                    }
                                }.onSuccess {
                                    AuthenticationConsentScreen(
                                        spName = page.recipientName,
                                        spLocation = page.recipientLocation,
                                        spImage = null,
                                        authenticationRequestParametersFrom = request.toAuthenticationRequestParametersFrom(),
                                        authenticationResponseResult = result.toAuthenticationResponseResult(),
                                        fromQrCodeScanner = page.fromQrCodeScanner,
                                        navigateUp = navigateUp,
                                        navigateToAuthenticationSuccessPage = {
                                            navigationStack.push(
                                                uiProvider.navigationPages.authenticationSuccessPage()
                                            )
                                        },
                                        walletMain = walletMain,
                                    )
                                }
                        }.onFailure {
                            LaunchedEffect(true) {
                                navigateUp()
                                walletMain.errorService.emit(it)
                            }
                        }
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


@OptIn(ExperimentalResourceApi::class)
private enum class NavigationData(
    val title: StringResource,
    val icon: @Composable () -> Unit,
    val destination: @Composable () -> Page,
    val isActive: (Page) -> Boolean
) {
    HOME_SCREEN(
        title = Res.string.navigation_button_label_my_data,
        icon = {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
            )
        },
        destination = {
            LocalUiProvider.current.navigationPages.homePage()
        },
        isActive = {
            when (it) {
                is HomePage -> true
                else -> false
            }
        },
    ),
    AUTHENTICATION_SCANNING_SCREEN(
        title = Res.string.navigation_button_label_show_data,
        icon = {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = null,
            )
        },
        destination = {
            LocalUiProvider.current.navigationPages.authenticationQrCodeScannerPage()
        },
        isActive = {
            when (it) {
                is AuthenticationQrCodeScannerPage -> true
                else -> false
            }
        },
    ),
    INFORMATION_SCREEN(
        title = Res.string.navigation_button_label_settings,
        icon = {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
            )
        },
        destination = {
            LocalUiProvider.current.navigationPages.settingsPage()
        },
        isActive = {
            when (it) {
                is SettingsPage -> true
                else -> false
            }
        },
    ),
}