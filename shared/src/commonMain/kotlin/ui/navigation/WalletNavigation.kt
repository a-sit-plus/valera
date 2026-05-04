package ui.navigation

import AppTestTags
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
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import at.asitplus.catchingUnwrapped
import at.asitplus.dcapi.issuance.DigitalCredentialOfferReturn
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.info_text_error_action_return_to_invoker
import at.asitplus.valera.resources.snackbar_reset_app_successfully
import at.asitplus.wallet.app.common.ErrorService
import at.asitplus.wallet.app.common.IntentState
import at.asitplus.wallet.app.common.KeystoreService
import at.asitplus.wallet.app.common.SnackbarService
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.decodeImage
import at.asitplus.wallet.app.common.data.SettingsRepository
import at.asitplus.wallet.app.common.domain.platform.UrlOpener
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.ktor.openid.CredentialIssuanceResult
import data.storage.StoreEntryId
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.compose.koinInject
import org.koin.core.scope.Scope
import ui.composables.BottomBar
import ui.composables.NavigationData
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
import ui.views.iso.holder.HolderView
import ui.views.iso.verifier.VerifierView
import ui.views.presentation.PresentationView

internal object NavigatorTestTags {
    const val loadingTestTag = "loadingTestTag"
}

@ExperimentalMaterial3Api
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
    var pendingRoute by remember { mutableStateOf<Route?>(null) }
    val scope = rememberCoroutineScope()

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
        scope.launch {
            Napier.d("Navigate back")
            val navigated = navController.navigateUpOnMain()
            if (!navigated) {
                Napier.w("Navigate up failed")
            }
        }
    }

    val navigatePending: () -> Unit = {
        scope.launch {
            pendingRoute?.let {
                Napier.d("Replace current with $it")
                if (navController.replaceCurrentOnMain(it)) {
                    pendingRoute = null
                } else {
                    navigateBack()
                }
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
                        true -> {
                            navController.navigateOnMain(route)
                        }

                        false -> {
                            pendingRoute = route
                            navController.navigateOnMain(CapabilitiesRoute(route.prerequisites))
                        }
                    }
                }

                else -> {
                    Napier.d("Navigate to: $route")
                    navController.navigateOnMain(route)
                }
            }
        }
    }

    val popBackStack: (Route) -> Unit = { route ->
        scope.launch {
            Napier.d("popBackStack: $route")
            navController.popBackStackOnMain(route)
        }
    }

    val navigateNewGraph: (Route) -> Unit = { route ->
        scope.launch {
            Napier.d("navigateNewGraph: $route")
            navController.navigateOnMain(route) {
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
        intentState.dcapiInvocationData.value != null
    }

    val invocationAwareBackHandler: () -> Unit = {
        if (shouldFinishToCaller()) {
            intentState.finishApp?.invoke() ?: navigateBack()
        } else {
            navigateBack()
        }
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

    val returnToHome: () -> Unit = {
        scope.launch {
            if (hasHomeScreenInBackStack()) {
                popBackStack(HomeScreenRoute)
            } else {
                navigateNewGraph(HomeScreenRoute)
            }
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
            invocationAwareBackHandler,
            popBackStack,
            navigatePending,
            navigateNewGraph,
            onClickLogo,
            shouldFinishToCaller,
            onError = { e ->
                returnToHome()
                errorService.emit(e)
            },
            koinScope = koinScope,
            intentState = intentState,
            returnToHome = returnToHome
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
                val dcapiReady = intentState.dcapiInvocationData.value != null
                Napier.d("WalletNavigation appLink dcapiReady=$dcapiReady")
                if (isDcapiLink && !dcapiReady) {
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
                navigate(ErrorRoute)
            }
        }
    }
}

@ExperimentalMaterial3Api
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun WalletNavHost(
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
    settingsRepository: SettingsRepository = koinInject(),
    intentState: IntentState,
    returnToHome: () -> Unit,
) {

    val items by walletMain.credentialValidityService.refreshItems.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val isOnRefreshCenter = backStackEntry?.destination?.hasRoute<RefreshCenterRoute>() == true

    var processedItemIds by remember { mutableStateOf(setOf<Long>()) }
    var hasNavigatedToCenter by remember { mutableStateOf(false) }

    LaunchedEffect(items.isEmpty()) {
        if (items.isEmpty()) {
            processedItemIds = emptySet()
            hasNavigatedToCenter = false
        }
    }

    if (!isOnRefreshCenter && items.size == 1) {
        val item = items.first()
        if (!processedItemIds.contains(item.storeEntryId)) {
            RefreshConfirmationDialog(
                entry = item.entry,
                onConfirm = {
                    processedItemIds = processedItemIds + item.storeEntryId
                    walletMain.credentialValidityService.refreshSingleWithStatus(item)
                    navController.navigate(RefreshCenterRoute) { launchSingleTop = true }
                },
                onDismiss = {
                    walletMain.credentialValidityService.removeRefreshRequest(item)
                }
            )
        }
    }

    if (!isOnRefreshCenter && items.size > 1 && !hasNavigatedToCenter) {
        RefreshConfirmationDialog(
            entry = null,
            onConfirm = {
                hasNavigatedToCenter = true
                navController.navigate(RefreshCenterRoute) { launchSingleTop = true }
            },
            onDismiss = {
                walletMain.credentialValidityService.clearAllRefreshRequests()
            }
        )
    }

    LaunchedEffect(items.size, isOnRefreshCenter) {
        if (isOnRefreshCenter && items.isEmpty()) {
            navController.popBackStack()
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
    ) {

        composable<RefreshCenterRoute> {
            RefreshCredentialsView(
                items = items,
                onRefreshItem = { item ->
                    walletMain.credentialValidityService.refreshSingleWithStatus(item)
                },
                onRemoveItem = { entry ->
                    walletMain.credentialValidityService.removeRefreshRequest(entry)
                },
                onDone = {
                    walletMain.credentialValidityService.clearAllRefreshRequests()
                }
            )
        }

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
                koinScope = koinScope,
                onRefresh = walletMain.credentialValidityService::refreshSingle
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
                },
            )
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

        composable<AuthenticationViewRoute> {
            DefaultPresentationGraphView(
                onError = onError,
                onClickLogo = onClickLogo,
                onClickSettings = {
                    navigate(SettingsRoute)
                },
                koinScope = koinScope,
                onNavigateUp = invocationAwareBackHandler,
            )
        }

        composable<DCAPIPresentationViewRoute> {
            DCAPIPresentationGraphView(
                onError = onError,
                onClickLogo = onClickLogo,
                onClickSettings = {
                    navigate(SettingsRoute)
                },
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
                Napier.d("Showing presentation view")
                PresentationView(
                    vm,
                    onPresentmentComplete = {
                        returnToHome()
                    },
                    coroutineScope = walletMain.scope,
                    walletMain.snackbarService,
                    onError = { e ->
                        returnToHome()
                        walletMain.errorService.emit(e)
                    }
                )
            }
        }

        composable<AuthenticationSuccessRoute> { backStackEntry ->
            AuthenticationSuccessView(
                navigateUp = invocationAwareBackHandler,
                onClickLogo = onClickLogo,
                onClickSettings = { navigate(SettingsRoute) }
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
            var vm by remember { mutableStateOf<LoadCredentialViewModel?>(null) }
            LaunchedEffect(Unit) {
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
                }.onSuccess { vm = it }
                 .onFailure {
                    returnToHome()
                    walletMain.errorService.emit(it)
                }
            }
            vm?.let { LoadCredentialView(it) } ?: LoadingView()
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
                                    transactionCode = transactionCode?.ifEmpty { null }
                                        ?.ifBlank { null },
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
                        resetStackOverride = {
                            intentState.finishApp?.invoke() ?: navigateBack()
                        },
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
                onClickSettings = { returnToHome() },
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
                    returnToHome()
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
                        walletMain.scope.launch {
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
                        Napier.d("valid creation request")
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
                        returnToHome()
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
                        returnToHome()
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
            QrCodeScannerView(
                koinScope = koinScope,
                onNavigateUp = navigateBack,
                onClickLogo = onClickLogo,
                onClickSettings = { navigate(SettingsRoute) },
                onNavigateToRoute = {
                    navigateBack()
                    navigate(it)
                },
                onError = {
                    walletMain.errorService.emit(it)
                },
            )
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
                    onContinue = {
                        navigatePending()
                    },
                    onNavigateUp = {
                        returnToHome()
                    },
                    prerequisites = prerequisites,
                )
            }
        }
    }
}
