import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.oidc.AuthenticationRequestParametersFrom
import at.asitplus.wallet.lib.oidc.helpers.AuthorizationResponsePreparationState
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.navigation_button_label_my_data
import composewalletapp.shared.generated.resources.navigation_button_label_settings
import composewalletapp.shared.generated.resources.navigation_button_label_show_data
import composewalletapp.shared.generated.resources.snackbar_reset_app_successfully
import composewalletapp.shared.generated.resources.navigation_button_label_check
import composewalletapp.shared.generated.resources.navigation_button_label_check
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
import ui.navigation.CustomDataRetrievalPage
import ui.navigation.CredentialDetailsPage
import ui.navigation.HomePage
import ui.navigation.LoadRequestedDataPage
import ui.navigation.LogPage
import ui.navigation.NavigationStack
import ui.navigation.Page
import ui.navigation.ProvisioningLoadingPage
import ui.navigation.QrDeviceEngagementPage
import ui.navigation.SelectDataRetrievalPage
import ui.navigation.RefreshCredentialsPage
import ui.navigation.SettingsPage
import ui.screens.AddCredentialScreen
import ui.screens.AuthenticationConsentScreen
import ui.screens.AuthenticationQrCodeScannerScreen
import ui.screens.AuthenticationSuccessScreen
import ui.screens.CustomDataRetrievalScreen
import ui.screens.CredentialDetailsScreen
import ui.screens.CustomDataRetrievalScreen
import ui.screens.ErrorScreen
import ui.screens.LoadRequestedDataScreen
import ui.screens.LoadingScreen
import ui.screens.LogScreen
import ui.screens.MyCredentialsScreen
import ui.screens.OnboardingWrapper
import ui.screens.ProvisioningLoadingScreen
import ui.screens.QrDeviceEngagementScreen
import ui.screens.SelectDataRetrievalScreen
import ui.screens.SettingsScreen
import view.AuthenticationQrCodeScannerViewModel

internal object NavigatorTestTags {
    const val postOnboardingContent = "postOnboardingContent"
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
                        Exception(pars["error_description"] ?: "Unknown Exception")
                    )
                }
                appLink.value = null
                return@LaunchedEffect
            }

            if (walletMain.provisioningService.redirectUri?.let { link.contains(it) } == true) {
                walletMain.provisioningService.redirectUri = null
                mainNavigationStack.push(
                    ProvisioningLoadingPage(
                        link = link
                    )
                )
                appLink.value = null
                return@LaunchedEffect
            }

            // if this is not for provisioning, it must be an authorization request
            kotlin.run {
                val consentPageBuilder =
                    BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(
                        oidcSiopWallet = walletMain.presentationService.oidcSiopWallet
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
    Scaffold(
        bottomBar = {
            val (_, page) = navigationStack.lastWithIndex()
            val pageNavigationData = when (page) {
                is HomePage -> NavigationData.HOME_SCREEN

                is SettingsPage -> NavigationData.INFORMATION_SCREEN

                is SelectDataRetrievalPage -> NavigationData.VERIFIER_SELECTION_SCREEN

                else -> null
            }

            if (pageNavigationData != null) {
                NavigationBar {
                    for (route in listOf(
                        NavigationData.HOME_SCREEN,
                        NavigationData.AUTHENTICATION_SCANNING_SCREEN,
                        NavigationData.VERIFIER_SELECTION_SCREEN,
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
                                navigationStack.push(AddCredentialPage())
                            },
                            navigateToCredentialDetailsPage = {
                                navigationStack.push(CredentialDetailsPage(it))
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

                    is CredentialDetailsPage -> {
                        CredentialDetailsScreen(
                            storeEntryId = page.storeEntryId,
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
                                navigationStack.push(AuthenticationLoadingPage())
                            },
                            authenticationQrCodeScannerViewModel = AuthenticationQrCodeScannerViewModel(
                                oidcSiopWallet = walletMain.presentationService.oidcSiopWallet,
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
                                AuthenticationRequestParametersFrom.deserialize(page.authenticationRequestParametersFromSerialized)
                                    .getOrThrow()
                            val preparationState =
                                vckJsonSerializer.decodeFromString<AuthorizationResponsePreparationState>(
                                    page.authorizationPreparationStateSerialized,
                                )

                            AuthenticationConsentScreen(
                                spName = page.recipientName,
                                spLocation = page.recipientLocation,
                                spImage = null,
                                authenticationRequestParametersFrom = request,
                                authorizationResponsePreparationState = preparationState,
                                fromQrCodeScanner = page.fromQrCodeScanner,
                                navigateUp = navigateUp,
                                navigateToAuthenticationSuccessPage = {
                                    navigationStack.push(
                                        AuthenticationSuccessPage()
                                    )
                                },
                                walletMain = walletMain,
                            )
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

                    is SelectDataRetrievalPage -> {
                        SelectDataRetrievalScreen(
                            navigateToCustomSelectionPage = {
                                navigationStack.push(CustomDataRetrievalPage())
                            },
                            navigateToQrDeviceEngagementPage = { doc ->
                                navigationStack.push(QrDeviceEngagementPage(doc))
                            }
                        )
                    }
                    is CustomDataRetrievalPage -> {
                        CustomDataRetrievalScreen(
                            navigateUp = navigateUp,
                            navigateToQrDeviceEngagementPage = { document ->
                                navigationStack.back()
                                navigationStack.push(QrDeviceEngagementPage(document))
                            },
                        )
                    }
                    is QrDeviceEngagementPage -> {
                        QrDeviceEngagementScreen(
                            navigateUp = navigateUp,
                            onFoundPayload = {payload ->
                                navigationStack.back()
                                navigationStack.push(LoadRequestedDataPage(page.document, payload))
                            }
                        )
                    }
                    is LoadRequestedDataPage -> {
                        LoadRequestedDataScreen(
                            document = page.document,
                            payload = page.payload,
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
    val destination: Page,
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
        destination = HomePage(),
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
        destination = AuthenticationQrCodeScannerPage(),
        isActive = {
            when (it) {
                is AuthenticationQrCodeScannerPage -> true
                else -> false
            }
        },
    ),
    VERIFIER_SELECTION_SCREEN(
        title = Res.string.navigation_button_label_check,
        icon = {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
            )
        },
        destination = SelectDataRetrievalPage(),
        isActive = {
            when (it) {
                is SelectDataRetrievalPage -> true
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
        destination = SettingsPage(),
        isActive = {
            when (it) {
                is SettingsPage -> true
                else -> false
            }
        },
    ),
}