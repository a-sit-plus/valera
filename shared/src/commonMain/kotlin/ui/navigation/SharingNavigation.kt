package ui.navigation

import ErrorHandlingOverrideException
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import at.asitplus.catchingUnwrapped
import at.asitplus.dcapi.issuance.DigitalCredentialOfferReturn
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.info_text_error_action_return_to_invoker
import at.asitplus.valera.resources.snackbar_reset_app_successfully
import at.asitplus.wallet.app.common.ErrorService
import at.asitplus.wallet.app.common.IntentState
import at.asitplus.wallet.app.common.SnackbarService
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.domain.platform.UrlOpener
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.ktor.openid.CredentialIssuanceResult
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.compose.koinInject
import org.koin.core.scope.Scope
import ui.navigation.IntentService.Companion.CREATE_CREDENTIAL_INTENT
import ui.navigation.IntentService.Companion.GET_CREDENTIAL_INTENT
import ui.navigation.routes.*
import ui.navigation.routes.RoutePrerequisites.CRYPTO
import ui.presentation.DCAPIPresentationGraphView
import ui.presentation.DefaultPresentationGraphView
import ui.viewmodels.*
import ui.viewmodels.authentication.PresentationViewModel
import ui.viewmodels.intents.*
import ui.views.*
import ui.views.authentication.AuthenticationSuccessView
import ui.views.intents.*
import ui.views.presentation.PresentationView

@ExperimentalMaterial3Api
@Composable
fun SharingNavigation(
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
    var pendingRoute by remember { mutableStateOf<Route?>(null) }
    val scope = rememberCoroutineScope()

    val initialLink = remember {
        intentState.appLink.value.also { link ->
            Napier.d("SharingNavigation initialLink=$link")
            // appLink is intentionally NOT cleared here.
            // combineTransform in LaunchedEffect(koinScope) processes it after appReady=true,
            // which guarantees dcapiInvocationData is visible before vm.process() runs.
        }
    }

    val navigateBack: () -> Unit = {
        scope.launch {
            if (!navController.navigateUp()) {
                intentState.finishApp?.invoke()
            }
        }
    }

    val navigatePending: () -> Unit = {
        scope.launch {
            pendingRoute?.let {
                Napier.d("Replace current with $it")
                navController.navigate(it) {
                    popUpTo(navController.currentDestination?.id ?: return@navigate) { inclusive = true }
                    launchSingleTop = true
                }
                pendingRoute = null
            } ?: run {
                navigateBack()
            }
        }
    }

    val navigate: (Route) -> Unit = { route ->
        scope.launch {
            when (route) {
                is PrerequisiteRoute -> {
                    when (walletMain.capabilitiesService.evaluatePrerequisites(route.prerequisites).first()) {
                        true -> navController.navigate(route)
                        false -> {
                            pendingRoute = route
                            navController.navigate(CapabilitiesRoute(route.prerequisites))
                        }
                    }
                }
                else -> {
                    Napier.d("SharingNavigation navigate: $route")
                    navController.navigate(route)
                }
            }
        }
    }

    val popBackStack: (Route) -> Unit = { route ->
        scope.launch {
            navController.popBackStack(route = route, inclusive = false)
        }
    }

    val navigateNewGraph: (Route) -> Unit = { route ->
        scope.launch {
            navController.navigate(route) {
                popUpTo(0)
                launchSingleTop = true
            }
        }
    }

    val onClickLogo = { urlOpener("https://wallet.a-sit.at/") }

    val shouldFinishToCaller: () -> Boolean = {
        intentState.dcapiInvocationData.value != null
    }

    val invocationAwareBackHandler: () -> Unit = {
        if (shouldFinishToCaller()) {
            intentState.finishApp?.invoke() ?: navigateBack()
        } else {
            navigateBack()
        }
    }

    val returnToHome: () -> Unit = {
        intentState.finishApp?.invoke() ?: navigateBack()
    }

    // Always start on LoadingRoute. The combineTransform in LaunchedEffect(koinScope) below
    // navigates to the real destination once appReady=true, ensuring all dependencies
    // (including dcapiInvocationData) are visible before any ViewModel processes them.
    val startDestination: Route = LoadingRoute

    LaunchedEffect(Unit) {
        if (initialLink == null) {
            intentState.finishApp?.invoke()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { _ ->
        SharingNavHost(
            navController = navController,
            startDestination = startDestination,
            navigate = navigate,
            navigateBack = navigateBack,
            invocationAwareBackHandler = invocationAwareBackHandler,
            popBackStack = popBackStack,
            navigatePending = navigatePending,
            navigateNewGraph = navigateNewGraph,
            onClickLogo = onClickLogo,
            shouldFinishToCaller = shouldFinishToCaller,
            onError = { e ->
                returnToHome()
                errorService.emit(e)
            },
            koinScope = koinScope,
            intentState = intentState,
            returnToHome = returnToHome,
        )
    }

    LaunchedEffect(koinScope) {
        walletMain.scope.launch {
            Napier.d("SharingNavigation appReady emit")
            walletMain.appReady.emit(true)
        }
        this.launch {
            intentState.appLink.combineTransform(walletMain.appReady) { link, ready ->
                Napier.d("SharingNavigation appLink combine link=$link ready=$ready")
                if (ready != true || link == null) return@combineTransform
                val isDcapiLink = link == GET_CREDENTIAL_INTENT || link == CREATE_CREDENTIAL_INTENT
                val dcapiReady = intentState.dcapiInvocationData.value != null
                if (isDcapiLink && !dcapiReady) return@combineTransform
                emit(link)
            }.collect { link ->
                catchingUnwrapped {
                    val route = intentService.handleIntent(link)
                    // Replace LoadingRoute with the real destination, so pressing back
                    // returns to the invoker rather than flashing LoadingView.
                    navigateNewGraph(route)
                }.onFailure {
                    errorService.emit(it)
                }
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
                if (ready == true) emit(error)
            }.collect {
                navigate(ErrorRoute)
            }
        }
    }
}

@ExperimentalMaterial3Api
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SharingNavHost(
    navController: NavHostController,
    startDestination: Route,
    navigate: (Route) -> Unit,
    navigateBack: () -> Unit,
    invocationAwareBackHandler: () -> Unit,
    popBackStack: (Route) -> Unit,
    navigatePending: () -> Unit,
    navigateNewGraph: (Route) -> Unit,
    onClickLogo: () -> Unit,
    shouldFinishToCaller: () -> Boolean,
    onError: (Throwable) -> Unit,
    koinScope: Scope,
    walletMain: WalletMain = koinInject(scope = koinScope),
    intentState: IntentState,
    returnToHome: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
    ) {

        composable<AuthorizationIntentRoute> { backStackEntry ->
            AuthorizationIntentView(remember {
                AuthorizationIntentViewModel(
                    walletMain = walletMain,
                    uri = backStackEntry.toRoute<AuthorizationIntentRoute>().uri,
                    onSuccess = { route ->
                        navigateNewGraph(route)
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
                        Napier.d("valid DCAPI authentication request")
                        navigateNewGraph(route)
                    },
                    onFailure = { e ->
                        val wrapped = ErrorHandlingOverrideException(
                            resetStackOverride = {
                                intentState.finishApp?.invoke() ?: navigateBack()
                            },
                            actionDescriptionOverride = Res.string.info_text_error_action_return_to_invoker,
                            onAcknowledge = (e as? ErrorHandlingOverrideException)?.onAcknowledge,
                            cause = (e as? ErrorHandlingOverrideException)?.cause ?: e
                        )
                        walletMain.errorService.emit(wrapped)
                    })
            })
        }

        composable<DCAPIIssuingIntentRoute> { backStackEntry ->
            DCAPIIssuingIntentView(remember {
                DCAPIIssuingIntentViewModel(
                    walletMain = walletMain,
                    uri = backStackEntry.toRoute<DCAPIIssuingIntentRoute>().uri,
                    onSuccess = { route ->
                        Napier.d("valid DCAPI creation request")
                        navigateNewGraph(route)
                    },
                    onFailure = { e ->
                        val overrideException = ErrorHandlingOverrideException(
                            resetStackOverride = {
                                intentState.finishApp?.invoke() ?: navigateBack()
                            },
                            actionDescriptionOverride = Res.string.info_text_error_action_return_to_invoker,
                            onAcknowledge = {
                                walletMain.platformAdapter.prepareDCAPIIssuingResponse(
                                    e.message ?: "invalid request", false
                                )
                            },
                            cause = e
                        )
                        walletMain.errorService.emit(overrideException)
                    })
            })
        }

        composable<PresentationIntentRoute> { backStackEntry ->
            PresentationIntentView(remember {
                PresentationIntentViewModel(
                    walletMain = walletMain,
                    intentState = intentState,
                    uri = backStackEntry.toRoute<PresentationIntentRoute>().uri,
                    onSuccess = { route ->
                        Napier.d("valid presentation request")
                        navigateNewGraph(route)
                    },
                    onFailure = {
                        walletMain.errorService.emit(Exception("Invalid Presentation Request"))
                    })
            })
        }

        composable<AuthenticationViewRoute> {
            DefaultPresentationGraphView(
                onError = onError,
                onClickLogo = onClickLogo,
                onClickSettings = { navigate(SettingsRoute) },
                koinScope = koinScope,
                onNavigateUp = invocationAwareBackHandler,
            )
        }

        composable<DCAPIPresentationViewRoute> {
            DCAPIPresentationGraphView(
                onError = onError,
                onClickLogo = onClickLogo,
                onClickSettings = { navigate(SettingsRoute) },
                koinScope = koinScope,
                onNavigateUp = invocationAwareBackHandler,
            )
        }

        composable<LocalPresentationAuthenticationConsentRoute> { backStackEntry ->
            val vm = remember {
                try {
                    intentState.presentationStateModel.value?.let {
                        PresentationViewModel(
                            presentationStateModel = it,
                            navigateUp = { returnToHome() },
                            onAuthenticationSuccess = { },
                            navigateToHomeScreen = { returnToHome() },
                            walletMain = walletMain,
                            onClickLogo = onClickLogo,
                            onClickSettings = { navigate(SettingsRoute) })
                    } ?: throw IllegalStateException("No presentation view model set")
                } catch (e: Throwable) {
                    returnToHome()
                    walletMain.errorService.emit(e)
                    null
                }
            }

            if (vm != null) {
                PresentationView(
                    vm,
                    onPresentmentComplete = { returnToHome() },
                    coroutineScope = walletMain.scope,
                    walletMain.snackbarService,
                    onError = { e ->
                        returnToHome()
                        walletMain.errorService.emit(e)
                    }
                )
            }
        }

        composable<AuthenticationSuccessRoute> {
            AuthenticationSuccessView(
                navigateUp = invocationAwareBackHandler,
                onClickLogo = onClickLogo,
                onClickSettings = { navigate(SettingsRoute) }
            )
        }

        composable<AddCredentialWithLinkRoute> { backStackEntry ->
            var vm by remember { mutableStateOf<LoadCredentialViewModel?>(null) }
            LaunchedEffect(Unit) {
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
                                        transactionCode = transactionCode?.ifEmpty { null }?.ifBlank { null },
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
                }.onSuccess { vm = it }
                 .onFailure {
                    returnToHome()
                    walletMain.errorService.emit(it)
                }
            }
            vm?.let { LoadCredentialView(it) } ?: LoadingView()
        }

        composable<AddCredentialPreAuthnRoute> { backStackEntry ->
            val offer = backStackEntry.toRoute<AddCredentialPreAuthnRoute>().credentialOffer
            var vm by remember { mutableStateOf<LoadCredentialViewModel?>(null) }
            LaunchedEffect(Unit) {
                runCatching {
                    LoadCredentialViewModel.init(
                        walletMain = walletMain,
                        navigateUp = navigateBack,
                        offer = offer,
                        onSubmit = { credentialIdentifierInfo, transactionCode, _ ->
                            returnToHome()
                            navigate(LoadingRoute)
                            walletMain.scope.launch {
                                try {
                                    walletMain.provisioningService.loadCredentialWithOffer(
                                        credentialOffer = offer,
                                        credentialIdentifierInfo = credentialIdentifierInfo,
                                        transactionCode = transactionCode?.ifEmpty { null }?.ifBlank { null },
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
                }.onSuccess { vm = it }
                 .onFailure {
                    returnToHome()
                    walletMain.errorService.emit(it)
                }
            }
            vm?.let { LoadCredentialView(it) } ?: LoadingView()
        }

        composable<AddCredentialDcApiRoute> { backStackEntry ->
            val offer = backStackEntry.toRoute<AddCredentialDcApiRoute>().credentialOffer
            var vm by remember { mutableStateOf<LoadCredentialViewModel?>(null) }
            LaunchedEffect(Unit) {
                runCatching {
                    lateinit var dcapiVm: LoadCredentialViewModel
                    val onSubmit: CredentialSelection = { credentialIdentifierInfo, transactionCode, _ ->
                        navigate(LoadingRoute)
                        walletMain.scope.launch {
                            try {
                                val issuanceResult = walletMain.provisioningService.loadCredentialWithOffer(
                                    credentialOffer = offer,
                                    credentialIdentifierInfo = credentialIdentifierInfo,
                                    transactionCode = transactionCode?.ifEmpty { null }?.ifBlank { null },
                                    authorizationServerMetadata = offer.authorizationServerMetadata
                                )
                                if (issuanceResult is CredentialIssuanceResult.Success) {
                                    navigate(AddCredentialDcApiSuccessRoute)
                                } else {
                                    dcapiVm.handleDCAPIIssuingResult(false, null)
                                }
                            } catch (e: Throwable) {
                                dcapiVm.handleDCAPIIssuingResult(false, e)
                            }
                        }
                    }
                    LoadCredentialViewModel.initFromDcApi(
                        walletMain = walletMain,
                        navigateUp = { intentState.finishApp?.invoke() ?: navigateBack() },
                        offer = offer,
                        onSubmit = onSubmit,
                        onClickLogo = onClickLogo,
                        onClickSettings = { navigate(SettingsRoute) }
                    ).also { dcapiVm = it }
                }.onSuccess { vm = it }
                 .onFailure {
                    val wrapped = ErrorHandlingOverrideException(
                        resetStackOverride = { intentState.finishApp?.invoke() ?: navigateBack() },
                        actionDescriptionOverride = Res.string.info_text_error_action_return_to_invoker,
                        onAcknowledge = {
                            if (walletMain.platformAdapter.hasPendingDCAPIIssuingRequest()) {
                                val response = vckJsonSerializer.encodeToString(
                                    DigitalCredentialOfferReturn.error(status = "offer_declined")
                                )
                                walletMain.platformAdapter.prepareDCAPIIssuingResponse(response, false)
                            }
                            intentState.finishApp?.invoke() ?: navigateBack()
                        },
                        cause = it
                    )
                    walletMain.errorService.emit(wrapped)
                }
            }
            vm?.let { LoadCredentialView(it) } ?: LoadingView()
        }

        composable<AddCredentialDcApiSuccessRoute> {
            val onAcknowledge = {
                if (walletMain.platformAdapter.hasPendingDCAPIIssuingRequest()) {
                    val response = vckJsonSerializer.encodeToString(DigitalCredentialOfferReturn.success())
                    walletMain.platformAdapter.prepareDCAPIIssuingResponse(response, true)
                }
                invocationAwareBackHandler()
            }

            BackHandler(onBack = onAcknowledge)

            CredentialAddedView(
                onAutoDismiss = onAcknowledge,
                onClickButton = onAcknowledge,
                onClickLogo = onClickLogo,
                onClickSettings = { navigate(SettingsRoute) }
            )
        }

        composable<ErrorIntentRoute> { backStackEntry ->
            ErrorIntentView(remember {
                ErrorIntentViewModel(
                    walletMain = walletMain,
                    uri = backStackEntry.toRoute<ErrorIntentRoute>().uri,
                    onFailure = { error -> walletMain.errorService.emit(error) })
            })
        }

        composable<ErrorRoute> {
            walletMain.errorService.error.collectAsState(null).value?.let {
                catchingUnwrapped {
                    val throwable = if (shouldFinishToCaller()) {
                        val existingOverride = it.throwable as? ErrorHandlingOverrideException
                        if (existingOverride?.hasUiOverride == true) {
                            existingOverride
                        } else {
                            ErrorHandlingOverrideException(
                                resetStackOverride = {
                                    intentState.finishApp?.invoke() ?: navigateBack()
                                },
                                actionDescriptionOverride = Res.string.info_text_error_action_return_to_invoker,
                                onAcknowledge = existingOverride?.onAcknowledge,
                                cause = existingOverride?.cause ?: it.throwable
                            )
                        }
                    } else {
                        it.throwable
                    }
                    ErrorViewModel(
                        clearError = { walletMain.errorService.clear() },
                        resetStack = { returnToHome() },
                        resetApp = {
                            walletMain.scope.launch {
                                walletMain.resetApp()
                            }
                            returnToHome()
                        },
                        throwable = throwable,
                        onClickLogo = onClickLogo,
                        onClickSettings = { navigate(SettingsRoute) })
                }.onSuccess {
                    ErrorView(remember { it })
                }.onFailure {
                    returnToHome()
                }
            }
        }

        composable<LoadingRoute> {
            LoadingView()
        }

        composable<SettingsRoute> {
            SettingsView(
                buildType = walletMain.buildContext.buildType,
                version = walletMain.buildContext.versionName,
                onClickShareLogFile = { navigate(LogRoute) },
                onClickLogo = onClickLogo,
                onClickSettings = { returnToHome() },
                onClickBack = navigateBack,
                onClickFAQs = null,
                onClickDataProtectionPolicy = null,
                onClickLicenses = null,
                onReset = {
                    walletMain.scope.launch { walletMain.resetApp() }
                    returnToHome()
                },
                koinScope = koinScope
            )
        }

        composable<LogRoute> {
            LogView(vm = remember {
                LogViewModel(
                    navigateUp = navigateBack,
                    walletMain = walletMain,
                    onClickLogo = onClickLogo,
                    onClickSettings = { navigate(SettingsRoute) })
            })
        }

        composable<CapabilitiesRoute> { backStackEntry ->
            backStackEntry.toRoute<CapabilitiesRoute>().prerequisites.let { prerequisites ->
                if (prerequisites.contains(CRYPTO)) {
                    BackHandler(enabled = true, onBack = {})
                } else {
                    BackHandler(enabled = true, onBack = { returnToHome() })
                }
                CapabilityView(
                    koinScope = koinScope,
                    onClickLogo = onClickLogo,
                    onClickSettings = { navigate(SettingsRoute) },
                    onContinue = { navigatePending() },
                    onNavigateUp = { returnToHome() },
                    prerequisites = prerequisites,
                )
            }
        }
    }
}