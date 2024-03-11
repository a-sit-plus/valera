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
import at.asitplus.wallet.lib.oidc.AuthenticationRequestParameters
import at.asitplus.wallet.lib.oidvci.decodeFromUrlQuery
import domain.ExtractClaimsFromPresentationDefinitionUseCase
import domain.RetrieveRelyingPartyMetadataFromAuthenticationQrCodeUseCase
import domain.RetrieveRequestRedirectFromAuthenticationQrCodeUseCase
import io.github.aakira.napier.Napier
import io.ktor.http.Url
import io.ktor.http.parseQueryString
import io.ktor.util.flattenEntries
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import navigation.AuthenticationConsentPage
import navigation.AuthenticationLoadingPage
import navigation.AuthenticationQrCodeScannerPage
import navigation.AuthenticationSuccessPage
import navigation.HomePage
import navigation.LogPage
import navigation.NavigationStack
import navigation.Page
import navigation.ProvisioningLoadingPage
import navigation.RefreshCredentialsPage
import navigation.SettingsPage
import view.AuthenticationConsentScreen
import view.AuthenticationQrCodeScannerScreen
import view.AuthenticationQrCodeScannerViewModel
import view.AuthenticationSuccessScreen
import view.ErrorScreen
import view.LoadDataScreen
import view.LoadingScreen
import view.LogScreen
import view.MyCredentialsScreen
import view.OnboardingWrapper
import view.ProvisioningLoadingScreen
import view.SettingsScreen

private enum class NavigationData(
    val title: String,
    val icon: @Composable () -> Unit,
    val destination: Page,
    val isActive: (Page) -> Boolean
) {
    HOME_SCREEN(
        title = Resources.NAVIGATION_BUTTON_LABEL_MY_DATA,
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
        title = Resources.NAVIGATION_BUTTON_LABEL_SHOW_DATA,
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
        title = Resources.NAVIGATION_BUTTON_LABEL_SETTINGS,
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
                walletMain.errorService.emit(
                    Exception(
                        pars["error_description"] ?: Resources.UNKNOWN_EXCEPTION
                    )
                )
                appLink.value = null
                return@LaunchedEffect
            }

            val host = walletMain.walletConfig.host.first()
            if (link.contains("$host/mobile")) {
                Napier.d("authentication request")
                val params = kotlin.runCatching {
                    Url(link).parameters.flattenEntries().toMap()
                        .decodeFromUrlQuery<AuthenticationRequestParameters>()
                }

                val requestedClaims = params.getOrNull()?.presentationDefinition?.let {
                    ExtractClaimsFromPresentationDefinitionUseCase().invoke(it)
                } ?: listOf()

                mainNavigationStack.push(
                    AuthenticationConsentPage(
                        url = link,
                        claims = requestedClaims,
                        recipientName = "DemoService",
                        recipientLocation = params.getOrNull()?.clientId ?: "DemoLocation",
                        fromQrCodeScanner = false
                    )
                )
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
                                Text(route.title)
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
                                runBlocking { walletMain.resetApp() }
                                navigationStack.reset()
                                walletMain.snackbarService.showSnackbar(Resources.SNACKBAR_RESET_APP_SUCCESSFULLY)
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
                                retrieveRelyingPartyMetadataFromAuthenticationQrCodeUseCase = RetrieveRelyingPartyMetadataFromAuthenticationQrCodeUseCase(
                                    client = walletMain.httpService.buildHttpClient(),
                                ),
                                retrieveRequestRedirectFromAuthenticationQrCodeUseCase = RetrieveRequestRedirectFromAuthenticationQrCodeUseCase(
                                    client = walletMain.httpService.buildHttpClient(),
                                ),
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