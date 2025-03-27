package ui.navigation

import AppTestTags
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import appLink
import at.asitplus.catchingUnwrapped
import at.asitplus.openid.AuthenticationRequestParameters
import at.asitplus.openid.CredentialOffer
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.openid.odcJsonSerializer
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.snackbar_clear_log_successfully
import at.asitplus.valera.resources.snackbar_reset_app_successfully
import at.asitplus.wallet.app.common.NavigationEnum
import at.asitplus.wallet.app.common.NavigationService
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.dcapi.DCAPIRequest
import at.asitplus.wallet.app.common.decodeImage
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.getString
import ui.composables.BottomBar
import ui.composables.NavigationData
import ui.navigation.routes.AddCredentialPreAuthnRoute
import ui.navigation.routes.AddCredentialRoute
import ui.navigation.routes.AuthenticationQrCodeScannerRoute
import ui.navigation.routes.AuthenticationSuccessRoute
import ui.navigation.routes.AuthenticationViewRoute
import ui.navigation.routes.CredentialDetailsRoute
import ui.navigation.routes.DCAPIAuthenticationConsentRoute
import ui.navigation.routes.ErrorRoute
import ui.navigation.routes.HomeScreenRoute
import ui.navigation.routes.LoadCredentialRoute
import ui.navigation.routes.LoadingRoute
import ui.navigation.routes.LogRoute
import ui.navigation.routes.OnboardingInformationRoute
import ui.navigation.routes.OnboardingStartRoute
import ui.navigation.routes.OnboardingTermsRoute
import ui.navigation.routes.OnboardingWrapperTestTags
import ui.navigation.routes.PreAuthQrCodeScannerRoute
import ui.navigation.routes.Route
import ui.navigation.routes.SettingsRoute
import ui.navigation.routes.SigningQtspSelectionRoute
import ui.navigation.routes.SigningRoute
import ui.screens.SelectIssuingServerView
import ui.viewmodels.AddCredentialViewModel
import ui.viewmodels.CredentialDetailsViewModel
import ui.viewmodels.CredentialsViewModel
import ui.viewmodels.ErrorViewModel
import ui.viewmodels.LoadCredentialViewModel
import ui.viewmodels.LogViewModel
import ui.viewmodels.PreAuthQrCodeScannerViewModel
import ui.viewmodels.SettingsViewModel
import ui.viewmodels.SigningQtspSelectionViewModel
import ui.viewmodels.SigningViewModel
import ui.viewmodels.authentication.AuthenticationQrCodeScannerViewModel
import ui.viewmodels.authentication.AuthenticationSuccessViewModel
import ui.viewmodels.authentication.DCAPIAuthenticationViewModel
import ui.viewmodels.authentication.DefaultAuthenticationViewModel
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
import ui.views.SigningQtspSelectionView
import ui.views.SigningView
import ui.views.authentication.AuthenticationQrCodeScannerView
import ui.views.authentication.AuthenticationSuccessView
import ui.views.authentication.AuthenticationView

internal object NavigatorTestTags {
    const val loadingTestTag = "loadingTestTag"
}

@Composable
fun WalletNavigation(walletMain: WalletMain) {
    val navController: NavHostController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()

    LaunchedEffect(null) {
        InitializeNavigationCollectors(
            navigationService = walletMain.navigationService,
            navController = navController
        )
    }

    val onClickLogo = {
        walletMain.platformAdapter.openUrl("https://wallet.a-sit.at/")
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val isConditionsAccepted = walletMain.walletConfig.isConditionsAccepted.collectAsState(null)

    val startDestination = when (isConditionsAccepted.value) {
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
        WalletNavHost(
            navController,
            startDestination,
            walletMain,
            backStackEntry,
            onClickLogo
        )
    }

    LaunchedEffect(null) {
        InitializeServiceCollectors(walletMain, snackbarHostState)
    }
}

@Composable
private fun WalletNavHost(
    navController: NavHostController,
    startDestination: Route,
    walletMain: WalletMain,
    backStackEntry: NavBackStackEntry?,
    onClickLogo: () -> Unit
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
                onClickStart = { walletMain.navigationService.navigate(OnboardingInformationRoute) },
                onClickLogo = onClickLogo,
                modifier = Modifier.testTag(OnboardingWrapperTestTags.onboardingStartScreen)
            )
        }
        composable<OnboardingInformationRoute> {
            OnboardingInformationView(
                onClickContinue = { walletMain.navigationService.navigate(OnboardingTermsRoute) },
                onClickLogo = onClickLogo
            )
        }
        composable<OnboardingTermsRoute> {
            OnboardingTermsView(
                onClickAccept = { walletMain.walletConfig.set(isConditionsAccepted = true) },
                onClickNavigateBack = { walletMain.navigationService.navigateBack() },
                onClickReadDataProtectionPolicy = {},
                onClickReadGeneralTermsAndConditions = {},
                onClickLogo = onClickLogo
            )
        }
        composable<HomeScreenRoute> {
            val vm = remember {
                CredentialsViewModel(
                    walletMain,
                    navigateToAddCredentialsPage = {
                        walletMain.navigationService.navigate(AddCredentialRoute)
                    },
                    navigateToQrAddCredentialsPage = {
                        walletMain.navigationService.navigate(PreAuthQrCodeScannerRoute)
                    },
                    navigateToCredentialDetailsPage = {
                        walletMain.navigationService.navigate(CredentialDetailsRoute(it))
                    },
                    imageDecoder = {
                        try {
                            walletMain.platformAdapter.decodeImage(it)
                        } catch (throwable: Throwable) {
                            // TODO: should this be emitted to the error service?
                            Napier.w("Failed Operation: decodeImage")
                            null
                        }
                    },
                    onClickLogo = onClickLogo
                )
            }
            CredentialsView(
                vm = vm,
                bottomBar = {
                    BottomBar(
                        navigate = { route -> walletMain.navigationService.navigate(route) },
                        selected = NavigationData.HOME_SCREEN
                    )
                }
            )
            walletMain.intentService.readyForIntents.value = true
        }
        composable<AuthenticationQrCodeScannerRoute> {
            val vm = remember {
                AuthenticationQrCodeScannerViewModel(
                    navigateUp = { walletMain.navigationService.navigateBack() },
                    onSuccess = { route ->
                        walletMain.navigationService.navigate(route)
                    },
                    walletMain = walletMain,
                    onClickLogo = onClickLogo
                )
            }
            AuthenticationQrCodeScannerView(vm)
        }
        composable<AuthenticationViewRoute> { backStackEntry ->
            val route: AuthenticationViewRoute = backStackEntry.toRoute()

            val vm = remember {
                try {
                    val request = odcJsonSerializer
                        .decodeFromString<RequestParametersFrom<AuthenticationRequestParameters>>(
                            route.authenticationRequestParametersFromSerialized
                        )

                    DefaultAuthenticationViewModel(
                        spName = null,
                        spLocation = route.recipientLocation,
                        spImage = null,
                        authenticationRequest = request,
                        navigateUp = { walletMain.navigationService.navigateBack() },
                        navigateToAuthenticationSuccessPage = {
                            walletMain.navigationService.navigate(AuthenticationSuccessRoute)
                        },
                        navigateToHomeScreen = {
                            walletMain.navigationService.popBackStack(HomeScreenRoute)
                        },
                        walletMain = walletMain,
                        onClickLogo = onClickLogo
                    )
                } catch (e: Throwable) {
                    walletMain.navigationService.popBackStack(HomeScreenRoute)
                    walletMain.errorService.emit(e)
                    null
                }
            }

            if (vm != null) {
                AuthenticationView(
                    vm = vm,
                    onError = { e ->
                        walletMain.navigationService.popBackStack(HomeScreenRoute)
                        walletMain.errorService.emit(e)
                    }
                )
            }
        }

        composable<DCAPIAuthenticationConsentRoute> { backStackEntry ->
            val route: DCAPIAuthenticationConsentRoute = backStackEntry.toRoute()

            val vm = remember {
                try {
                    val dcApiRequest =
                        DCAPIRequest.deserialize(route.apiRequestSerialized).getOrThrow()

                    DCAPIAuthenticationViewModel(
                        dcApiRequest = dcApiRequest,
                        navigateUp = { walletMain.navigationService.navigateBack() },
                        navigateToAuthenticationSuccessPage = {
                            walletMain.navigationService.navigate(AuthenticationSuccessRoute)
                        },
                        walletMain = walletMain,
                        navigateToHomeScreen = {
                            walletMain.navigationService.popBackStack(HomeScreenRoute)
                        },
                        onClickLogo = onClickLogo
                    )
                } catch (e: Throwable) {
                    walletMain.navigationService.popBackStack(HomeScreenRoute)
                    walletMain.errorService.emit(e)
                    null
                }
            }

            if (vm != null) {
                AuthenticationView(
                    vm = vm,
                    onError = { e ->
                        walletMain.navigationService.popBackStack(HomeScreenRoute)
                        walletMain.errorService.emit(e)
                    },
                )
            }

        }

        composable<AuthenticationSuccessRoute> { backStackEntry ->
            val vm = remember {
                AuthenticationSuccessViewModel(
                    navigateUp = { walletMain.navigationService.navigateBack() },
                    onClickLogo = onClickLogo
                )
            }
            AuthenticationSuccessView(vm = vm)
        }

        composable<AddCredentialRoute> { backStackEntry ->
            val vm = remember {
                AddCredentialViewModel(
                    walletMain = walletMain,
                    navigateUp = { walletMain.navigationService.navigateBack() },
                    hostString = runBlocking { walletMain.walletConfig.host.first() },
                    onSubmitServer = { host ->
                        walletMain.navigationService.navigate(LoadCredentialRoute(host))
                    },
                    onClickLogo = onClickLogo
                )
            }
            SelectIssuingServerView(vm)
        }

        composable<LoadCredentialRoute> { backStackEntry ->
            val route: LoadCredentialRoute = backStackEntry.toRoute()
            val vm = remember {
                try {
                    LoadCredentialViewModel(
                        walletMain = walletMain,
                        navigateUp = { walletMain.navigationService.navigateBack() },
                        hostString = route.host,
                        onSubmit = { credentialIdentifierInfo, requestedAttributes, _ ->
                            walletMain.navigationService.popBackStack(HomeScreenRoute)
                            walletMain.scope.launch {
                                walletMain.startProvisioning(
                                    host = route.host,
                                    credentialIdentifierInfo = credentialIdentifierInfo,
                                    requestedAttributes = requestedAttributes,
                                ) {
                                }
                            }

                        },
                        onClickLogo = onClickLogo
                    )
                } catch (e: Throwable) {
                    walletMain.navigationService.popBackStack(HomeScreenRoute)
                    walletMain.errorService.emit(e)
                    null
                }
            }
            if (vm != null) {
                LoadCredentialView(vm)
            }
        }

        composable<AddCredentialPreAuthnRoute> { backStackEntry ->
            val route: AddCredentialPreAuthnRoute = backStackEntry.toRoute()
            val offer = Json.decodeFromString<CredentialOffer>(route.credentialOfferSerialized)
            val vm = remember {
                LoadCredentialViewModel(
                    walletMain = walletMain,
                    navigateUp = { walletMain.navigationService.navigateBack() },
                    offer = offer,
                    onSubmit = { credentialIdentifierInfo, requestedAttributes, transactionCode ->
                        walletMain.navigationService.popBackStack(HomeScreenRoute)
                        walletMain.navigationService.navigate(LoadingRoute)
                        walletMain.scope.launch {
                            try {
                                walletMain.provisioningService.loadCredentialWithOffer(
                                    credentialOffer = offer,
                                    credentialIdentifierInfo = credentialIdentifierInfo,
                                    transactionCode = transactionCode?.ifEmpty { null }
                                        ?.ifBlank { null },
                                    requestedAttributes = requestedAttributes
                                )
                                walletMain.navigationService.popBackStack(HomeScreenRoute)
                            } catch (e: Throwable) {
                                walletMain.navigationService.popBackStack(HomeScreenRoute)
                                walletMain.errorService.emit(e)
                            }
                        }
                    },
                    onClickLogo = onClickLogo
                )
            }
            LoadCredentialView(vm)
        }

        composable<CredentialDetailsRoute> { backStackEntry ->
            val route: CredentialDetailsRoute = backStackEntry.toRoute()
            val vm = remember {
                CredentialDetailsViewModel(
                    storeEntryId = route.storeEntryId,
                    navigateUp = { walletMain.navigationService.navigateBack() },
                    walletMain = walletMain,
                    onClickLogo = onClickLogo
                )
            }
            CredentialDetailsView(vm = vm)
        }

        composable<SettingsRoute> { backStackEntry ->
            val vm = remember {
                SettingsViewModel(
                    onClickShareLogFile = {
                        walletMain.navigationService.navigate(LogRoute)
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
                    onClickSigning = {
                        walletMain.navigationService.navigate(SigningQtspSelectionRoute)
                    },
                    walletMain = walletMain,
                    onClickLogo = onClickLogo
                )
            }
            SettingsView(
                vm = vm,
                bottomBar = {
                    BottomBar(
                        navigate = { route -> walletMain.navigationService.navigate(route) },
                        selected = NavigationData.INFORMATION_SCREEN
                    )
                })
        }

        composable<PreAuthQrCodeScannerRoute> { backStackEntry ->
            val vm = remember {
                PreAuthQrCodeScannerViewModel(
                    walletMain = walletMain,
                    navigateUp = { walletMain.navigationService.navigateBack() },
                    navigateToAddCredentialsPage = { offer ->
                        walletMain.navigationService.navigate(
                            AddCredentialPreAuthnRoute(
                                Json.encodeToString(
                                    offer
                                )
                            )
                        )
                    },
                    onClickLogo = onClickLogo
                )
            }
            PreAuthQrCodeScannerScreen(vm)
        }

        composable<LogRoute> { backStackEntry ->
            val vm = remember {
                LogViewModel(
                    navigateUp = { walletMain.navigationService.navigateBack() },
                    walletMain = walletMain,
                    onClickLogo = onClickLogo
                )
            }
            LogView(vm = vm)
        }

        composable<ErrorRoute> { backStackEntry ->
            val route: ErrorRoute = backStackEntry.toRoute()
            val vm = remember {
                ErrorViewModel(
                    resetStack = { walletMain.navigationService.popBackStack(HomeScreenRoute) },
                    message = route.message,
                    cause = route.cause,
                    onClickLogo = onClickLogo
                )
            }
            ErrorView(vm)
        }

        composable<LoadingRoute> { backStackEntry ->
            LoadingView()
        }

        composable<AuthenticationQrCodeScannerRoute> { backStackEntry ->
            val vm = remember {
                AuthenticationQrCodeScannerViewModel(
                    navigateUp = { walletMain.navigationService.navigateBack() },
                    onSuccess = { route ->
                        walletMain.navigationService.navigateBack()
                        walletMain.navigationService.navigate(route)
                    },
                    walletMain = walletMain,
                    onClickLogo = onClickLogo
                )
            }
            AuthenticationQrCodeScannerView(vm)
        }

        composable<SigningRoute> { backStackEntry ->
            val vm = remember {
                SigningViewModel(
                    navigateUp = { walletMain.navigationService.navigateBack() },
                    createSignRequest = { signRequest ->
                        walletMain.navigationService.navigate(HomeScreenRoute)
                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                walletMain.signingService.start(signRequest)

                            } catch (e: Throwable) {
                                walletMain.errorService.emit(e)
                            }
                        }
                    },
                    walletMain = walletMain,
                    onClickLogo = onClickLogo
                )
            }
            SigningView(vm)
        }
        composable<SigningQtspSelectionRoute> { backStackEntry ->
            val vm = remember {
                SigningQtspSelectionViewModel(
                    navigateUp = { walletMain.navigationService.navigateBack() },
                    onContinue = {
                        walletMain.navigationService.navigate(SigningRoute)
                    },
                    walletMain = walletMain,
                    onClickLogo = onClickLogo
                )
            }
            SigningQtspSelectionView(vm = vm)
        }
    }
}

fun InitializeNavigationCollectors(
    navigationService: NavigationService,
    navController: NavHostController
) {
    CoroutineScope(Dispatchers.Main).launch {
        navigationService.navigate.collect { (route, method) ->
            when (method) {
                NavigationEnum.Navigate -> {
                    route?.let { navController.navigate(route) }
                }
                NavigationEnum.NavigateBack -> {
                    navController.navigateUp()
                }
                NavigationEnum.PopBackStack -> {
                    route?.let {
                        navController.popBackStack(
                            route = route,
                            inclusive = false
                        )
                    }
                }
            }
        }
    }
}

fun InitializeServiceCollectors(
    walletMain: WalletMain,
    snackbarHostState: SnackbarHostState
) {
    CoroutineScope(Dispatchers.Default).launch {
        this.launch {
            walletMain.snackbarService.message.collect { (text, actionLabel, callback) ->
                val result = snackbarHostState.showSnackbar(text, actionLabel, true)
                when (result) {
                    SnackbarResult.Dismissed -> {}
                    SnackbarResult.ActionPerformed -> callback?.invoke()
                }
            }
        }
        this.launch {
            walletMain.errorService.error.collect { (message, cause) ->
                walletMain.navigationService.navigate(ErrorRoute(message, cause))
            }
        }
        this.launch {
            appLink.combineTransform(walletMain.intentService.readyForIntents) { link, ready ->
                if (ready == true && link != null) {
                    emit(link)
                }
            }.collect { link ->
                Napier.d("appLink.combineTransform $link")
                catchingUnwrapped {
                    walletMain.intentService.handleIntent(link)
                }.onFailure {
                    walletMain.errorService.emit(it)
                }
            }
        }
    }
}