package ui.navigation

import AppTestTags
import UncorrectableErrorException
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import at.asitplus.openid.AuthenticationRequestParameters
import at.asitplus.openid.CredentialOffer
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.openid.odcJsonSerializer
import at.asitplus.wallet.app.common.ErrorService
import at.asitplus.wallet.app.common.SnackbarService
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.decodeImage
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.snackbar_clear_log_successfully
import at.asitplus.valera.resources.snackbar_reset_app_successfully
import data.dcapi.DCAPIRequest
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.getString
import ui.composables.BottomBar
import ui.composables.NavigationData
import ui.navigation.Routes.APIAuthenticationConsentRoute
import ui.navigation.Routes.AddCredentialPreAuthnRoute
import ui.navigation.Routes.AddCredentialRoute
import ui.navigation.Routes.AuthenticationQrCodeScannerRoute
import ui.navigation.Routes.AuthenticationSuccessRoute
import ui.navigation.Routes.AuthenticationViewRoute
import ui.navigation.Routes.CredentialDetailsRoute
import ui.navigation.Routes.ErrorRoute
import ui.navigation.Routes.HomeScreenRoute
import ui.navigation.Routes.LoadCredentialRoute
import ui.navigation.Routes.LoadingRoute
import ui.navigation.Routes.LogRoute
import ui.navigation.Routes.OnboardingInformationRoute
import ui.navigation.Routes.OnboardingStartRoute
import ui.navigation.Routes.OnboardingTermsRoute
import ui.navigation.Routes.OnboardingWrapperTestTags
import ui.navigation.Routes.PreAuthQrCodeScannerRoute
import ui.navigation.Routes.Route
import ui.navigation.Routes.SettingsRoute
import ui.screens.SelectIssuingServerView
import ui.viewmodels.AddCredentialViewModel
import ui.viewmodels.Authentication.DCAPIAuthenticationViewModel
import ui.viewmodels.Authentication.AuthenticationQrCodeScannerViewModel
import ui.viewmodels.Authentication.AuthenticationSuccessViewModel
import ui.viewmodels.Authentication.DefaultAuthenticationViewModel
import ui.viewmodels.CredentialDetailsViewModel
import ui.viewmodels.CredentialsViewModel
import ui.viewmodels.LoadCredentialViewModel
import ui.viewmodels.LogViewModel
import ui.viewmodels.PreAuthQrCodeScannerViewModel
import ui.viewmodels.SettingsViewModel
import ui.views.Authentication.AuthenticationQrCodeScannerView
import ui.views.Authentication.AuthenticationSuccessView
import ui.views.Authentication.AuthenticationView
import ui.views.CredentialDetailsView
import ui.views.CredentialsView
import ui.views.ErrorView
import ui.views.LoadCredentialView
import ui.views.LoadingView
import ui.views.LogView
import ui.views.OnboardingInformationView
import ui.views.OnboardingStartView
import ui.views.OnboardingTermsView
import ui.views.PreAuthQrCodeScannerScreen
import ui.views.SettingsView

internal object NavigatorTestTags {
    const val loadingTestTag = "loadingTestTag"
}

@Composable
fun WalletNavigation(walletMain: WalletMain) {
    val navController: NavHostController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()

    val navigateBack: () -> Unit = {
        CoroutineScope(Dispatchers.Main).launch {
            Napier.d("Navigate back")
            navController.navigateUp()

        }
    }

    val navigate: (Route) -> Unit = { route ->
        CoroutineScope(Dispatchers.Main).launch {
            Napier.d("Navigate to: $route")
            navController.navigate(route)
        }
    }

    val popBackStack: (Route) -> Unit = { route ->
        CoroutineScope(Dispatchers.Main).launch {
            Napier.d("popBackStack: $route")
            navController.popBackStack(route = route, inclusive = false)
        }
    }

    handleIntent(walletMain = walletMain, navigate = navigate, navigateBack = navigateBack)

    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarService = SnackbarService(walletMain.scope, snackbarHostState)

    val startDestination: Route

    val errorService = ErrorService(showError = { message, cause -> navigate(ErrorRoute(message, cause)) })
    walletMain.errorService = errorService

    try {
        walletMain.initialize(snackbarService)
    } catch (e: Throwable) {
        walletMain.errorService.emit(UncorrectableErrorException(e))
    }

    val isConditionsAccepted = walletMain.walletConfig.isConditionsAccepted.collectAsState(null)

    startDestination = when (isConditionsAccepted.value) {
        true -> HomeScreenRoute
        false -> OnboardingStartRoute
        null -> LoadingRoute
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        modifier = Modifier.testTag(AppTestTags.rootScaffold)
    ) { _ ->
        WalletNavHost(navController, startDestination, navigate, walletMain, navigateBack, backStackEntry, popBackStack)
    }
}

@Composable
private fun WalletNavHost(
    navController: NavHostController,
    startDestination: Route,
    navigate: (Route) -> Unit,
    walletMain: WalletMain,
    navigateBack: () -> Unit,
    backStackEntry: NavBackStackEntry?,
    popBackStack: (Route) -> Unit
) {


    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        modifier = Modifier
            .fillMaxSize()
    ) {
        composable<OnboardingStartRoute> {
            OnboardingStartView(
                onClickStart = { navigate(OnboardingInformationRoute) },
                modifier = Modifier.testTag(OnboardingWrapperTestTags.onboardingStartScreen)
            )
        }
        composable<OnboardingInformationRoute> {
            OnboardingInformationView(onClickContinue = { navigate(OnboardingTermsRoute) })
        }
        composable<OnboardingTermsRoute> {
            OnboardingTermsView(onClickAccept = { walletMain.walletConfig.set(isConditionsAccepted = true) },
                onClickNavigateBack = { navigateBack() },
                onClickReadDataProtectionPolicy = {},
                onClickReadGeneralTermsAndConditions = {})
        }
        composable<HomeScreenRoute> {
            val vm = CredentialsViewModel(walletMain,
                navigateToAddCredentialsPage = {
                    navigate(AddCredentialRoute)
                },
                navigateToQrAddCredentialsPage = {
                    navigate(PreAuthQrCodeScannerRoute)
                },
                navigateToCredentialDetailsPage = {
                    navigate(CredentialDetailsRoute(it))
                },
                imageDecoder = {
                    try {
                        walletMain.platformAdapter.decodeImage(it)
                    } catch (throwable: Throwable) {
                        // TODO: should this be emitted to the error service?
                        Napier.w("Failed Operation: decodeImage")
                        null
                    }
                })
            CredentialsView(
                vm = vm,
                bottomBar = {
                    BottomBar(
                        navigate = navigate,
                        selected = NavigationData.HOME_SCREEN
                    )
                }
            )
        }
        composable<AuthenticationQrCodeScannerRoute> {
            val vm = AuthenticationQrCodeScannerViewModel(
                navigateUp = { navigateBack() },
                onSuccess = { route ->
                    navigate(route)
                },
                walletMain = walletMain
            )
            AuthenticationQrCodeScannerView(vm)
        }
        composable<AuthenticationViewRoute> { backStackEntry ->
            val route: AuthenticationViewRoute = backStackEntry.toRoute()

            val request = odcJsonSerializer
                .decodeFromString<RequestParametersFrom<AuthenticationRequestParameters>>(route.authenticationRequestParametersFromSerialized)

            val vm = DefaultAuthenticationViewModel(
                spName = null,
                spLocation = route.recipientLocation,
                spImage = null,
                authenticationRequest = request,
                navigateUp = { navigateBack() },
                navigateToAuthenticationSuccessPage = {
                    navigate(AuthenticationSuccessRoute)
                },
                navigateToHomeScreen = {
                    popBackStack(HomeScreenRoute)
                },
                walletMain = walletMain,
            )
            AuthenticationView(vm = vm)
        }

        composable<APIAuthenticationConsentRoute> { backStackEntry ->
            val route: APIAuthenticationConsentRoute = backStackEntry.toRoute()

            val vm = try {
                val dcApiRequest = DCAPIRequest.deserialize(route.apiRequestSerialized).getOrThrow()

                DCAPIAuthenticationViewModel(
                    dcApiRequest = dcApiRequest,
                    navigateUp = { navigateBack() },
                    navigateToAuthenticationSuccessPage = {
                        navigate(AuthenticationSuccessRoute)
                    },
                    walletMain = walletMain,
                    navigateToHomeScreen = {
                        popBackStack(HomeScreenRoute)
                    }
                )

            } catch (e: Throwable) {
                popBackStack(HomeScreenRoute)
                walletMain.errorService.emit(e)
                null
            }

            if (vm != null) {
                AuthenticationView(vm = vm)
            }

        }

        composable<AuthenticationSuccessRoute> { backStackEntry ->
            val vm = AuthenticationSuccessViewModel(navigateUp = { navigateBack() })
            AuthenticationSuccessView(vm = vm)
        }

        composable<AddCredentialRoute> { backStackEntry ->
            val vm = AddCredentialViewModel(
                walletMain = walletMain,
                navigateUp = navigateBack,
                hostString = runBlocking { walletMain.walletConfig.host.first() },
                onSubmitServer = { host ->
                    navigate(LoadCredentialRoute(host))
                })
            SelectIssuingServerView(vm)
        }

        composable<LoadCredentialRoute> { backStackEntry ->
            val route: LoadCredentialRoute = backStackEntry.toRoute()
            val vm = LoadCredentialViewModel(
                walletMain = walletMain,
                navigateUp = navigateBack,
                hostString = route.host,
                onSubmit = { credentialIdentifierInfo, requestedAttributes, _ ->
                    popBackStack(HomeScreenRoute)
                    walletMain.scope.launch {
                        walletMain.startProvisioning(
                            host = route.host,
                            credentialIdentifierInfo = credentialIdentifierInfo,
                            requestedAttributes = requestedAttributes,
                        ) {
                        }
                    }

                })
            LoadCredentialView(vm)
        }

        composable<AddCredentialPreAuthnRoute> { backStackEntry ->
            val route: AddCredentialPreAuthnRoute = backStackEntry.toRoute()
            val offer = Json.decodeFromString<CredentialOffer>(route.credentialOfferSerialized)
            val vm = LoadCredentialViewModel(
                walletMain = walletMain,
                navigateUp = navigateBack,
                offer = offer,
                onSubmit = { credentialIdentifierInfo, requestedAttributes, transactionCode ->
                    popBackStack(HomeScreenRoute)
                    navigate(LoadingRoute)
                    walletMain.scope.launch {
                        try {
                            walletMain.provisioningService.loadCredentialWithOffer(
                                credentialOffer = offer,
                                credentialIdentifierInfo = credentialIdentifierInfo,
                                transactionCode = transactionCode?.ifEmpty { null }?.ifBlank { null },
                                requestedAttributes = requestedAttributes
                            )
                        } catch (e: Throwable) {
                            popBackStack(HomeScreenRoute)
                            walletMain.errorService.emit(e)
                        }
                        popBackStack(HomeScreenRoute)
                    }
                }
            )
            LoadCredentialView(vm)
        }

        composable<CredentialDetailsRoute> { backStackEntry ->
            val route: CredentialDetailsRoute = backStackEntry.toRoute()
            val vm = CredentialDetailsViewModel(
                storeEntryId = route.storeEntryId,
                navigateUp = { navigateBack() },
                walletMain = walletMain,
            )
            CredentialDetailsView(vm = vm)
        }

        composable<SettingsRoute> { backStackEntry ->
            val vm = SettingsViewModel(
                onClickShareLogFile = {
                    navigate(LogRoute)
                },
                onClickResetApp = {
                    val resetMessage = runBlocking {
                        walletMain.resetApp()
                        getString(Res.string.snackbar_reset_app_successfully)
                    }
                    walletMain.snackbarService.showSnackbar(resetMessage)
                    navController.popBackStack(route = HomeScreenRoute, inclusive = false)
                },
                onClickClearLogFile = {
                    val clearMessage = runBlocking {
                        walletMain.clearLog()
                        getString(Res.string.snackbar_clear_log_successfully)
                    }
                    walletMain.snackbarService.showSnackbar(clearMessage)
                },
                walletMain = walletMain,
            )
            SettingsView(
                vm = vm,
                bottomBar = {
                    BottomBar(
                        navigate = navigate,
                        selected = NavigationData.INFORMATION_SCREEN
                    )
                })
        }

        composable<PreAuthQrCodeScannerRoute> { backStackEntry ->
            val vm = PreAuthQrCodeScannerViewModel(
                walletMain = walletMain,
                navigateUp = { navigateBack() },
                navigateToAddCredentialsPage = { offer ->
                    navigate(AddCredentialPreAuthnRoute(Json.encodeToString(offer)))
                })
            PreAuthQrCodeScannerScreen(vm)
        }

        composable<LogRoute> { backStackEntry ->
            val vm = LogViewModel(navigateUp = { navigateBack() }, walletMain = walletMain)
            LogView(vm = vm)
        }

        composable<ErrorRoute> { backStackEntry ->
            val route: ErrorRoute = backStackEntry.toRoute()
            ErrorView(resetStack = { popBackStack(HomeScreenRoute) }, message = route.message, cause = route.cause)
        }

        composable<LoadingRoute> { backStackEntry ->
            LoadingView()
        }

        composable<AuthenticationQrCodeScannerRoute> { backStackEntry ->
            val vm = AuthenticationQrCodeScannerViewModel(
                navigateUp = { navigateBack() },
                onSuccess = { route ->
                    navigateBack()
                    navigate(route)
                },
                walletMain = walletMain
            )
            AuthenticationQrCodeScannerView(vm)
        }
    }
}
