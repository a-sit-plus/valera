package ui.navigation

import AppTestTags
import DeferredErrorActionException
import ErrorHandlingOverrideException
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import at.asitplus.catching
import at.asitplus.catchingUnwrapped
import at.asitplus.dcapi.request.DCAPIWalletRequest
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.info_text_error_action_close_intent
import at.asitplus.valera.resources.info_text_error_action_return_to_invoker
import at.asitplus.valera.resources.snackbar_reset_app_successfully
import at.asitplus.wallet.app.common.ErrorService
import at.asitplus.wallet.app.common.IntentState
import at.asitplus.wallet.app.common.KeystoreService
import at.asitplus.wallet.app.common.SnackbarService
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.data.SettingsRepository
import at.asitplus.wallet.app.common.domain.platform.UrlOpener
import at.asitplus.wallet.lib.data.vckJsonSerializer
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.koin.compose.koinInject
import org.koin.core.scope.Scope
import ui.composables.BottomBar
import ui.composables.NavigationData
import ui.navigation.routes.*
import ui.navigation.routes.RoutePrerequisites.CRYPTO
import ui.viewmodels.*
import ui.viewmodels.authentication.AuthenticationViewModel
import ui.viewmodels.authentication.DefaultAuthenticationViewModel
import ui.viewmodels.authentication.NewDCAPIAuthenticationViewModel
import ui.viewmodels.authentication.PresentationViewModel
import ui.viewmodels.intents.*
import ui.views.*
import ui.views.authentication.AuthenticationSuccessView
import ui.views.authentication.AuthenticationView
import ui.views.intents.*
import ui.views.iso.holder.HolderView
import ui.views.iso.verifier.VerifierView
import ui.views.presentation.PresentationView

internal object NavigatorTestTags {
    const val loadingTestTag = "loadingTestTag"
}

@Composable
fun WalletNavigation(
    koinScope: Scope,
    intentState: IntentState,
    intentService: IntentService = koinInject(),
    snackbarService: SnackbarService = koinInject(),
    errorService: ErrorService = koinInject(scope = koinScope),
    walletMain: WalletMain = koinInject(scope = koinScope),
    urlOpener: UrlOpener = koinInject(),
) {
    val navController: NavHostController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingRoute: Route? = null
    val initialLink = remember {
        intentState.appLink.value.also { link ->
            Napier.d("WalletNavigation initialLink=$link")
            if (link != null) {
                Napier.d("WalletNavigation clearing initialLink")
                intentState.appLink.value = null
            }
        }
    }

    val navigateBack: () -> Unit = {
        CoroutineScope(Dispatchers.Main).launch {
            Napier.d("Navigate back")
            navController.navigateUp()
        }
    }

    val navigatePending: () -> Unit = {
        CoroutineScope(Dispatchers.Main).launch {
            pendingRoute?.let {
                Napier.d("Replace current with $it")
                navController.navigate(it) {
                    popUpTo(navController.currentDestination?.id ?: return@navigate) { inclusive = true }
                    launchSingleTop = true
                }
                pendingRoute = null
            } ?: run {
                Napier.d("Navigate back")
                navController.navigateUp()
            }
        }
    }

    val navigate: (Route) -> Unit = { route ->
        CoroutineScope(Dispatchers.Main).launch {
            when (route) {
                is PrerequisiteRoute -> {
                    when (walletMain.capabilitiesService.evaluatePrerequisites(route.prerequisites).first()) {
                        true -> {
                            navController.navigate(route)
                        }

                        false -> {
                            pendingRoute = route
                            navController.navigate(CapabilitiesRoute(route.prerequisites))
                        }
                    }
                }

                else -> {
                    Napier.d("Navigate to: $route")
                    navController.navigate(route)
                }
            }
        }
    }

    val popBackStack: (Route) -> Unit = { route ->
        CoroutineScope(Dispatchers.Main).launch {
            Napier.d("popBackStack: $route")
            navController.popBackStack(route = route, inclusive = false)
        }
    }

    val navigateNewGraph: (Route) -> Unit = { route ->
        CoroutineScope(Dispatchers.Main).launch {
            Napier.d("navigateNewGraph: $route")
            navController.navigate(route) {
                popUpTo(0)
                launchSingleTop = true
            }
        }
    }

    val onClickLogo = {
        urlOpener("https://wallet.a-sit.at/")
    }

    val hasHomeScreenInBackStack: () -> Boolean = {
        val route = HomeScreenRoute::class.qualifiedName
        try {
            navController.getBackStackEntry(route!!)
            true
        } catch (_: IllegalArgumentException) {
            false
        }
    }
    val shouldFinishToCaller: () -> Boolean = {
        intentState.isIntentActivity
    }

    val startDestination = remember(initialLink) {
        if (initialLink != null) {
            try {
                intentService.handleIntent(initialLink)
            } catch (e: Throwable) {
                Napier.e("Unable to parse intent link", e)
                InitializationRoute
            }
        } else {
            InitializationRoute
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }, modifier = Modifier.testTag(AppTestTags.rootScaffold)
    ) { _ ->
        WalletNavHost(
            navController,
            startDestination,
            navigate,
            navigateBack,
            popBackStack,
            navigatePending,
            navigateNewGraph,
            onClickLogo,
            hasHomeScreenInBackStack,
            shouldFinishToCaller,
            onError = { e ->
                popBackStack(HomeScreenRoute)
                errorService.emit(e)
            },
            koinScope = koinScope,
            intentState = intentState
        )
    }

    LaunchedEffect(koinScope) {
        if (initialLink != null) {
            walletMain.scope.launch {
                Napier.d("WalletNavigation appReady emit from initialLink")
                walletMain.appReady.emit(true)
            }
        }
        this.launch {
            intentState.appLink.combineTransform(walletMain.appReady) { link, ready ->
                Napier.d("WalletNavigation appLink combine link=$link ready=$ready")
                if (ready != true || link == null) {
                    return@combineTransform
                }
                val isDcapiLink = link == GET_CREDENTIAL_INTENT || link == CREATE_CREDENTIAL_INTENT
                if (isDcapiLink && intentState.dcapiInvocationData.value == null) {
                    Napier.d("WalletNavigation appLink waiting for dcapiInvocationData")
                    return@combineTransform
                }
                Napier.d("WalletNavigation appLink emitting link=$link")
                emit(link)
            }.collect { link ->
                Napier.d("appLink.combineTransform $link")
                catchingUnwrapped {
                    val route = intentService.handleIntent(link)
                    Napier.d("WalletNavigation handleIntent route=$route")
                    navigate(route)
                }.onFailure {
                    errorService.emit(it)
                }
                Napier.d("WalletNavigation clearing appLink after navigate")
                intentState.appLink.value = null
            }
        }
        this.launch {
            snackbarService.message.collect { (text, actionLabel, callback) ->
                when (snackbarHostState.showSnackbar(text, actionLabel, true)) {
                    SnackbarResult.Dismissed -> {}
                    SnackbarResult.ActionPerformed -> callback?.invoke()
                }
            }
        }
        this.launch {
            errorService.error.combineTransform(walletMain.appReady) { error, ready ->
                if (ready == true) {
                    emit(error)
                }
            }.collect {
                val override = it.throwable as? ErrorHandlingOverrideException
                if (override != null && override.onlyForIntentActivity && !intentState.isIntentActivity) {
                    return@collect
                }
                navigate(ErrorRoute)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun WalletNavHost(
    navController: NavHostController,
    startDestination: Route,
    navigate: (Route) -> Unit,
    navigateBack: () -> Unit,
    popBackStack: (Route) -> Unit,
    navigatePending: () -> Unit,
    navigateNewGraph: (Route) -> Unit,
    onClickLogo: () -> Unit,
    hasHomeScreenInBackStack: () -> Boolean,
    shouldFinishToCaller: () -> Boolean,
    onError: (Throwable) -> Unit,
    koinScope: Scope,
    walletMain: WalletMain = koinInject(scope = koinScope),
    settingsRepository: SettingsRepository = koinInject(),
    intentState: IntentState,

    ) {
    val returnToHome: () -> Unit = {
        if (hasHomeScreenInBackStack()) {
            popBackStack(HomeScreenRoute)
        } else {
            navigateNewGraph(HomeScreenRoute)
        }
    }
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        composable<InitializationRoute> {
            InitializationView(koinScope = koinScope, navigateOnboarding = {
                navigateNewGraph(OnboardingStartRoute)
            }, navigateHomeScreen = {
                navigateNewGraph(HomeScreenRoute)
            })
        }
        composable<OnboardingStartRoute> {
            catchingUnwrapped { KeystoreService.checkKeyMaterialValid() }.onFailure { Napier.d(it) { "Deleted old Key" } }
            OnboardingStartView(
                onClickStart = { navigate(OnboardingInformationRoute) },
                onClickLogo = onClickLogo,
                modifier = Modifier.testTag(OnboardingWrapperTestTags.onboardingStartScreen)
            )
        }
        composable<OnboardingInformationRoute> {
            OnboardingInformationView(
                onClickContinue = {
                    settingsRepository.set(isConditionsAccepted = true)
                    navigateNewGraph(InitializationRoute)
                }, onClickLogo = onClickLogo
            )
        }
        composable<HomeScreenRoute> {
            CredentialsView(
                navigateToAddCredentialsPage = {
                    navigate(AddCredentialRoute)
                },
                navigateToQrAddCredentialsPage = {
                    navigate(QrCodeScannerRoute(QrCodeScannerMode.PROVISIONING))
                },
                navigateToCredentialDetailsPage = {
                    navigate(CredentialDetailsRoute(it))
                },
                onClickLogo = onClickLogo,
                onClickSettings = { navigate(SettingsRoute) },
                bottomBar = {
                    BottomBar(
                        navigate = { route -> navigate(route) },
                        selected = NavigationData.HOME_SCREEN
                    )
                },
                koinScope = koinScope
            )
            LaunchedEffect(koinScope) {
                walletMain.scope.launch {
                    walletMain.appReady.emit(true)
                }
                walletMain.scope.launch {
                    catchingUnwrapped { KeystoreService.checkKeyMaterialValid() }.onFailure {
                        walletMain.errorService.emit(it)
                    }
                }
            }
        }

        composable<PresentDataRoute> {
            PresentDataView(
                onNavigateToAuthenticationQrCodeScannerView = {
                    navigate(QrCodeScannerRoute(QrCodeScannerMode.AUTHENTICATION))
                },
                onNavigateToProximityHolderView = { navigate(ProximityHolderRoute) },
                onClickLogo = onClickLogo,
                onClickSettings = { navigate(SettingsRoute) },
                bottomBar = {
                    BottomBar(
                        navigate = navigate, selected = NavigationData.PRESENT_DATA_SCREEN
                    )
                })
        }

        composable<ProximityHolderRoute> {
            HolderView(
                navigateUp = { navigate(PresentDataRoute) },
                onClickLogo = onClickLogo,
                onClickSettings = { navigate(SettingsRoute) },
                onNavigateToPresentmentScreen = {
                    intentState.presentationStateModel.value = it
                    navigate(LocalPresentationAuthenticationConsentRoute("QR"))
                },
                bottomBar = {
                    BottomBar(
                        navigate = navigate,
                        selected = NavigationData.PRESENT_DATA_SCREEN
                    )
                },
                onError = onError,
                koinScope = koinScope
            )
        }

        composable<ProximityVerifierRoute> {
            VerifierView(
                navigateUp = { navigateBack() },
                onClickLogo = onClickLogo,
                onClickSettings = { navigate(SettingsRoute) },
                onError = onError,
                bottomBar = {
                    BottomBar(
                        navigate = navigate,
                        selected = NavigationData.VERIFY_DATA_SCREEN
                    )
                },
                koinScope = koinScope
            )
        }

        composable<AuthenticationViewRoute> { backStackEntry ->
            val route: AuthenticationViewRoute = backStackEntry.toRoute()
            val navigateUpFromAuth = if (shouldFinishToCaller()) {
                { intentState.finishApp?.invoke() ?: navigateBack() }
            } else {
                navigateBack
            }

            val vm = remember {
                try {
                    val dcApiRequest = when (val request = route.authorizationResponsePreparationState.request) {
                        is RequestParametersFrom.DcApiSigned<*> -> request.dcApiRequest
                        is RequestParametersFrom.DcApiUnsigned<*> -> request.dcApiRequest
                        else -> null
                    }
                    val spLocation = dcApiRequest?.callingOrigin ?: route.recipientLocation

                    DefaultAuthenticationViewModel(
                        spName = dcApiRequest?.callingPackageName,
                        spLocation = spLocation,
                        spImage = null,
                        authenticationRequest = route.authenticationRequest,
                        preparationState = route.authorizationResponsePreparationState,
                        navigateUp = navigateUpFromAuth,
                        navigateToAuthenticationSuccessPage = {
                            navigate(AuthenticationSuccessRoute(it, route.isCrossDeviceFlow))
                        },
                        navigateToHomeScreen = {
                            popBackStack(HomeScreenRoute)
                        },
                        walletMain = walletMain,
                        onClickLogo = onClickLogo,
                        onClickSettings = { navigate(SettingsRoute) },
                    )
                } catch (e: Throwable) {
                    popBackStack(HomeScreenRoute)
                    walletMain.errorService.emit(e)
                    null
                }
            }

            if (vm != null) {
                AuthenticationView(
                    vm = vm, onError = onError
                )
            }
        }

        composable<DCAPIAuthenticationConsentRoute> { backStackEntry ->
            val navigateUpFromAuth = if (shouldFinishToCaller()) {
                { intentState.finishApp?.invoke() ?: navigateBack() }
            } else {
                navigateBack
            }
            val vm: AuthenticationViewModel? = remember {
                try {
                    val apiRequestSerialized =
                        backStackEntry.toRoute<DCAPIAuthenticationConsentRoute>().apiRequestSerialized
                    val dcApiWalletRequest: DCAPIWalletRequest.IsoMdoc = catching {
                        vckJsonSerializer.decodeFromString<DCAPIWalletRequest.IsoMdoc>(apiRequestSerialized)
                    }.getOrThrow()

                    NewDCAPIAuthenticationViewModel(
                        isoMdocRequest = dcApiWalletRequest,
                        navigateUp = navigateUpFromAuth,
                        navigateToAuthenticationSuccessPage = {
                            navigate(AuthenticationSuccessRoute(it, false))
                        },
                        walletMain = walletMain,
                        navigateToHomeScreen = {
                            popBackStack(HomeScreenRoute)
                        },
                        onClickLogo = onClickLogo,
                        onClickSettings = { navigate(SettingsRoute) }
                    ).also { it.initWithDeviceRequest(dcApiWalletRequest.isoMdocRequest.deviceRequest) }

                } catch (e: Throwable) {
                    Napier.e("error", e)
                    onError(e)
                    null
                }
            }

            if (vm != null) {
                AuthenticationView(
                    vm = vm,
                    onError = onError,
                )
            }
        }

        composable<LocalPresentationAuthenticationConsentRoute> { backStackEntry ->
            val vm = remember {
                try {
                    intentState.presentationStateModel.value?.let {
                        PresentationViewModel(
                            presentationStateModel = it,
                            navigateUp = { popBackStack(HomeScreenRoute) },
                            onAuthenticationSuccess = { },
                            navigateToHomeScreen = { popBackStack(HomeScreenRoute) },
                            walletMain = walletMain,
                            onClickLogo = onClickLogo,
                            onClickSettings = { navigate(SettingsRoute) })
                    } ?: throw IllegalStateException("No presentation view model set")
                } catch (e: Throwable) {
                    popBackStack(HomeScreenRoute)
                    walletMain.errorService.emit(e)
                    null
                }
            }

            if (vm != null) {
                Napier.d("Showing presentation view")
                PresentationView(
                    vm,
                    onPresentmentComplete = {
                        popBackStack(HomeScreenRoute)
                    },
                    coroutineScope = walletMain.scope,
                    walletMain.snackbarService,
                    onError = { e ->
                        popBackStack(HomeScreenRoute)
                        walletMain.errorService.emit(e)
                    })
            }
        }

        composable<AuthenticationSuccessRoute> { backStackEntry ->
            val navigateUpFromSuccess = if (shouldFinishToCaller()) {
                { intentState.finishApp?.invoke() ?: navigateBack() }
            } else {
                navigateBack
            }
            AuthenticationSuccessView(
                navigateUp = navigateUpFromSuccess,
                onClickLogo = onClickLogo,
                onClickSettings = { navigate(SettingsRoute) },
                koinScope = koinScope
            )
        }

        composable<AddCredentialRoute> {
            SelectIssuingServerView(
                navigateUp = navigateBack,
                onClickLogo = onClickLogo,
                onClickSettings = { navigate(SettingsRoute) },
                onNavigateToLoadCredentialRoute = { host ->
                    navigate(LoadCredentialRoute(host))
                },
                koinScope = koinScope
            )
        }

        composable<LoadCredentialRoute> { backStackEntry ->
            remember {
                runBlocking {
                    runCatching {
                        LoadCredentialViewModel.init(
                            walletMain = walletMain,
                            navigateUp = navigateBack,
                            hostString = backStackEntry.toRoute<LoadCredentialRoute>().host,
                            onSubmit = { credentialIdentifierInfo, _, _ ->
                                returnToHome()
                                walletMain.scope.launch {
                                    walletMain.startProvisioning(
                                        host = backStackEntry.toRoute<LoadCredentialRoute>().host,
                                        credentialIdentifierInfo = credentialIdentifierInfo,
                                    ) {}
                                }

                            },
                            onClickLogo = onClickLogo,
                            onClickSettings = { navigate(SettingsRoute) })
                    }.getOrElse {
                        returnToHome()
                        walletMain.errorService.emit(it)
                        null
                    }
                }
            }?.let { vm ->
                LoadCredentialView(vm)
            }
        }

        composable<AddCredentialWithLinkRoute> { backStackEntry ->
            remember {
                runBlocking {
                    runCatching {
                        LoadCredentialViewModel.init(
                            walletMain = walletMain,
                            navigateUp = navigateBack,
                            url = backStackEntry.toRoute<AddCredentialWithLinkRoute>().uri,
                            onSubmit = { credentialIdentifierInfo, transactionCode, offer ->
                                returnToHome()
                                navigate(LoadingRoute)
                                walletMain.scope.launch {
                                    try {
                                        walletMain.provisioningService.loadCredentialWithOffer(
                                            credentialOffer = offer!!,
                                            credentialIdentifierInfo = credentialIdentifierInfo,
                                            transactionCode = transactionCode?.ifEmpty { null }
                                                ?.ifBlank { null },
                                        )
                                        returnToHome()
                                    } catch (e: Throwable) {
                                        returnToHome()
                                        walletMain.errorService.emit(e)
                                    }
                                }
                            },
                            onClickLogo = onClickLogo,
                            onClickSettings = { navigate(SettingsRoute) }
                        )
                    }.getOrElse {
                        popBackStack(HomeScreenRoute)
                        walletMain.errorService.emit(it)
                        null
                    }
                }
            }?.let { vm ->
                LoadCredentialView(vm)
            }
        }

        composable<AddCredentialPreAuthnRoute> { backStackEntry ->
            val offer = backStackEntry.toRoute<AddCredentialPreAuthnRoute>().credentialOffer
            remember {
                runBlocking {
                    runCatching {
                        LoadCredentialViewModel.init(
                            walletMain = walletMain,
                            navigateUp = navigateBack,
                            offer = offer,
                            onSubmit = { credentialIdentifierInfo, transactionCode, _ ->
                                popBackStack(HomeScreenRoute)
                                navigate(LoadingRoute)
                                walletMain.scope.launch {
                                    try {
                                        walletMain.provisioningService.loadCredentialWithOffer(
                                            credentialOffer = offer,
                                            credentialIdentifierInfo = credentialIdentifierInfo,
                                            transactionCode = transactionCode?.ifEmpty { null }
                                                ?.ifBlank { null },
                                        )
                                        popBackStack(HomeScreenRoute)
                                    } catch (e: Throwable) {
                                        popBackStack(HomeScreenRoute)
                                        walletMain.errorService.emit(e)
                                    }
                                }
                            },
                            onClickLogo = onClickLogo,
                            onClickSettings = { navigate(SettingsRoute) })
                    }.getOrElse {
                        popBackStack(HomeScreenRoute)
                        walletMain.errorService.emit(it)
                        null
                    }
                }
            }?.let { vm ->
                LoadCredentialView(vm)
            }
        }

        composable<CredentialDetailsRoute> { backStackEntry ->
            CredentialDetailsView(vm = remember {
                CredentialDetailsViewModel(
                    storeEntryId = backStackEntry.toRoute<CredentialDetailsRoute>().storeEntryId,
                    navigateUp = navigateBack,
                    walletMain = walletMain,
                    onClickLogo = onClickLogo,
                    onClickSettings = { navigate(SettingsRoute) })
            })
        }

        composable<SettingsRoute> { backStackEntry ->
            SettingsView(
                buildType = walletMain.buildContext.buildType,
                version = walletMain.buildContext.versionName,
                onClickShareLogFile = {
                    navigate(LogRoute)
                },
                onClickLogo = onClickLogo,
                onClickSettings = { popBackStack(HomeScreenRoute) },
                onClickBack = navigateBack,
                onClickFAQs = null,
                onClickDataProtectionPolicy = null,
                onClickLicenses = null,
                onReset = { navigateNewGraph(InitializationRoute) },
                koinScope = koinScope
            )
        }

        composable<LogRoute> { backStackEntry ->
            LogView(vm = remember {
                LogViewModel(
                    navigateUp = navigateBack,
                    walletMain = walletMain,
                    onClickLogo = onClickLogo,
                    onClickSettings = { navigate(SettingsRoute) })
            })
        }

        composable<ErrorRoute> { backStackEntry ->
            walletMain.errorService.error.collectAsState(null).value?.let {
                catchingUnwrapped {
                    val throwable = if (it.throwable is ErrorHandlingOverrideException) {
                        it.throwable
                    } else if (shouldFinishToCaller()) {
                        ErrorHandlingOverrideException(
                            resetStackOverride = {
                                intentState.finishApp?.invoke() ?: navigateBack()
                            },
                            actionDescriptionOverride = Res.string.info_text_error_action_return_to_invoker,
                            onAcknowledge = (it.throwable as? DeferredErrorActionException)?.onAcknowledge,
                            onlyForIntentActivity = true,
                            cause = it.throwable
                        )
                    } else {
                        it.throwable
                    }
                    ErrorViewModel(
                        clearError = { walletMain.errorService.clear() },
                        resetStack = { popBackStack(HomeScreenRoute) },
                        resetApp = {
                            walletMain.scope.launch {
                                walletMain.resetApp()
                                val resetMessage =
                                    getString(Res.string.snackbar_reset_app_successfully)
                                walletMain.snackbarService.showSnackbar(resetMessage)
                                popBackStack(InitializationRoute)
                            }
                        },
                        throwable = throwable,
                        onClickLogo = onClickLogo,
                        onClickSettings = { navigate(SettingsRoute) })
                }.onSuccess {
                    ErrorView(remember { it })
                }.onFailure {
                    popBackStack(HomeScreenRoute)
                }
            }
        }

        composable<LoadingRoute> { backStackEntry ->
            LoadingView()
        }

        composable<SigningQtspSelectionRoute> { backStackEntry ->
            SigningQtspSelectionView(vm = remember {
                SigningQtspSelectionViewModel(
                    navigateUp = navigateBack,
                    onContinue = { signatureRequestParameters ->
                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                walletMain.signingService.start(signatureRequestParameters)
                            } catch (e: Throwable) {
                                walletMain.errorService.emit(e)
                            }
                        }
                    },
                    walletMain = walletMain,
                    onClickLogo = onClickLogo,
                    onClickSettings = { navigate(SettingsRoute) },
                    signatureRequestParameters = backStackEntry.toRoute<SigningQtspSelectionRoute>().signatureRequestParameters
                )
            })
        }

        composable<ProvisioningResumeIntentRoute> { backStackEntry ->
            ProvisioningIntentView(remember {
                ProvisioningIntentViewModel(
                    walletMain = walletMain,
                    uri = backStackEntry.toRoute<ProvisioningResumeIntentRoute>().uri,
                    onSuccess = {
                        navigateBack()
                    },
                    onFailure = { error ->
                        walletMain.errorService.emit(error)
                    })
            })
        }

        composable<AuthorizationIntentRoute> { backStackEntry ->
            AuthorizationIntentView(remember {
                AuthorizationIntentViewModel(
                    walletMain = walletMain,
                    uri = backStackEntry.toRoute<AuthorizationIntentRoute>().uri,
                    onSuccess = { route ->
                        navigateBack()
                        navigate(route)
                    },
                    onFailure = {
                        walletMain.errorService.emit(Exception("Invalid Authentication Request"))
                    })
            })
        }

        composable<DCAPIAuthorizationIntentRoute> { backStackEntry ->
            DCAPIAuthorizationIntentView(remember {
                DCAPIAuthorizationIntentViewModel(
                    walletMain = walletMain,
                    uri = backStackEntry.toRoute<DCAPIAuthorizationIntentRoute>().uri,
                    onSuccess = { route ->
                        Napier.d("valid authentication request")
                        navigateBack()
                        navigate(route)
                    },
                    onFailure = { e ->
                        val wrapped = ErrorHandlingOverrideException(
                            resetStackOverride = {
                                intentState.finishApp?.invoke() ?: navigateBack()
                            },
                            actionDescriptionOverride = Res.string.info_text_error_action_return_to_invoker,
                            onAcknowledge = (e as? DeferredErrorActionException)?.onAcknowledge,
                            onlyForIntentActivity = true,
                            cause = e
                        )
                        walletMain.errorService.emit(wrapped)
                    })
            })
        }

        composable<DCAPICreationIntentRoute> { backStackEntry ->
            // TODO implement DC API creation
            /*DCAPICreationIntentView(remember {
                DCAPICreationIntentViewModel(
                    walletMain = walletMain,
                    uri = backStackEntry.toRoute<DCAPICreationIntentRoute>().uri,
                    onSuccess = { route ->
                        Napier.d("valid authentication request")
                        navigateBack()
                        navigate(route)
                    },
                    onFailure = { e ->
                        walletMain.errorService.emit(e)
                    })
            })*/
        }

        composable<PresentationIntentRoute> { backStackEntry ->
            PresentationIntentView(remember {
                PresentationIntentViewModel(
                    walletMain = walletMain,
                    intentState = intentState,
                    uri = backStackEntry.toRoute<PresentationIntentRoute>().uri,
                    onSuccess = { route ->
                        Napier.d("valid presentation request")
                        navigateBack()
                        navigate(route)
                    },
                    onFailure = {
                        walletMain.errorService.emit(Exception("Invalid Presentation Request"))
                    })
            })
        }

        composable<SigningServiceIntentRoute> { backStackEntry ->
            SigningServiceIntentView(remember {
                SigningServiceIntentViewModel(
                    walletMain = walletMain,
                    uri = backStackEntry.toRoute<SigningServiceIntentRoute>().uri,
                    onSuccess = {
                        popBackStack(HomeScreenRoute)
                    },
                    onFailure = { error ->
                        walletMain.errorService.emit(error)
                    })
            })
        }

        composable<SigningPreloadIntentRoute> { backStackEntry ->
            SigningPreloadIntentView(
                remember {
                    SigningPreloadIntentViewModel(
                        walletMain = walletMain,
                        uri = backStackEntry.toRoute<SigningPreloadIntentRoute>().uri,
                        onSuccess = {
                            navigateBack()
                        },
                        onFailure = { error ->
                            walletMain.errorService.emit(error)
                        })
                })
        }

        composable<SigningCredentialIntentRoute> { backStackEntry ->
            SigningCredentialIntentView(remember {
                SigningCredentialIntentViewModel(
                    walletMain = walletMain,
                    uri = backStackEntry.toRoute<SigningCredentialIntentRoute>().uri,
                    onSuccess = {
                        popBackStack(HomeScreenRoute)
                    },
                    onFailure = { error ->
                        walletMain.errorService.emit(error)
                    })
            })
        }

        composable<SigningIntentRoute> { backStackEntry ->
            SigningIntentView(remember {
                SigningIntentViewModel(
                    walletMain = walletMain,
                    uri = backStackEntry.toRoute<SigningIntentRoute>().uri,
                    onSuccess = {
                        walletMain.scope.launch {
                            navigateBack()
                            navigate(
                                SigningQtspSelectionRoute(
                                    walletMain.signingService.parseSignatureRequestParameter(
                                        backStackEntry.toRoute<SigningIntentRoute>().uri
                                    )
                                )
                            )
                        }
                    },
                    onFailure = { error ->
                        walletMain.errorService.emit(error)
                    })
            })
        }

        composable<ErrorIntentRoute> { backStackEntry ->
            ErrorIntentView(
                remember {
                    ErrorIntentViewModel(
                        walletMain = walletMain,
                        uri = backStackEntry.toRoute<ErrorIntentRoute>().uri,
                        onFailure = { error ->
                            walletMain.errorService.emit(error)
                        })
                })
        }
        composable<QrCodeScannerRoute> { backStackEntry ->
            QrCodeScannerView(remember {
                QrCodeScannerViewModel(
                    navigateUp = navigateBack,
                    onSuccess = { route ->
                        navigateBack()
                        navigate(route)
                    },
                    onFailure = { error ->
                        walletMain.errorService.emit(error)
                    },
                    walletMain = walletMain,
                    onClickLogo = onClickLogo,
                    onClickSettings = { navigate(SettingsRoute) },
                    mode = backStackEntry.toRoute<QrCodeScannerRoute>().mode
                )
            })
        }
        composable<CapabilitiesRoute> { backStackEntry ->
            backStackEntry.toRoute<CapabilitiesRoute>().prerequisites.let { prerequisites ->
                if (prerequisites.contains(CRYPTO)) {
                    BackHandler(enabled = true, onBack = {})
                } else {
                    BackHandler(enabled = true, onBack = { popBackStack(HomeScreenRoute) })
                }
                CapabilityView(
                    koinScope = koinScope,
                    onClickLogo = onClickLogo,
                    onClickSettings = { navigate(SettingsRoute) },
                    onSoftReset = {
                        walletMain.scope.launch {
                            walletMain.softReset()
                            popBackStack(InitializationRoute)
                        }
                    },
                    onContinue = {
                        navigatePending()
                    },
                    onNavigateUp = {
                        popBackStack(HomeScreenRoute)
                    },
                    prerequisites = prerequisites,
                )
            }
        }
    }
}
