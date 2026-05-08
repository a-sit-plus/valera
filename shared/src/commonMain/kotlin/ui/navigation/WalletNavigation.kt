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
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
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
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.compose.koinInject
import org.koin.core.scope.Scope
import ui.composables.BottomBar
import ui.composables.NavigationData
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
    intentService: IntentService = koinInject(scope = koinScope),
    snackbarService: SnackbarService = koinInject(scope = koinScope),
    errorService: ErrorService = koinInject(scope = koinScope),
    walletMain: WalletMain = koinInject(scope = koinScope),
    urlOpener: UrlOpener = koinInject(scope = koinScope),
) {
    val navController: NavHostController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    // rememberCoroutineScope() ties navigation-triggered launches to the composition
    // lifetime, so they are automatically cancelled when the composable leaves the tree.
    // walletMain.scope is used only for business logic inside LaunchedEffect.
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

    val navigator: WalletNavigationController = remember(navController, scope) {
        WalletNavigationControllerImpl(
            navController = navController,
            scope = scope,
            intentState = intentState,
            capabilitiesService = walletMain.capabilitiesService,
        )
    }

    val onClickLogo = { urlOpener("https://wallet.a-sit.at/") }

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
            navController = navController,
            startDestination = startDestination,
            navigator = navigator,
            onClickLogo = onClickLogo,
            onError = { e ->
                navigator.returnToHome()
                errorService.emit(e)
            },
            koinScope = koinScope,
            intentState = intentState,
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
                Napier.d("WalletNavigation appLink emitting link=$link")
                emit(link)
            }.collect { link ->
                Napier.d("appLink.combineTransform $link")
                catchingUnwrapped {
                    val route = intentService.handleIntent(link)
                    Napier.d("WalletNavigation handleIntent route=$route")
                    navigator.navigateNewGraph(route)
                }.onFailure {
                    errorService.emit(it)
                }
                Napier.d("WalletNavigation clearing appLink after navigateNewGraph")
                intentState.appLink.value = null
            }
        }
        this.launch {
            snackbarService.message.collect { (text, actionLabel, duration, callback) ->
                when (snackbarHostState.showSnackbar(text, actionLabel, true, duration)) {
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
                navigator.navigate(ErrorRoute)
            }
        }
    }
}

@ExperimentalMaterial3Api
@Composable
private fun WalletNavHost(
    navController: NavHostController,
    startDestination: Route,
    navigator: WalletNavigationController,
    onClickLogo: () -> Unit,
    onError: (Throwable) -> Unit,
    koinScope: Scope,
    walletMain: WalletMain = koinInject(scope = koinScope),
    settingsRepository: SettingsRepository = koinInject(scope = koinScope),
    intentState: IntentState,
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
                navigator.navigateNewGraph(OnboardingStartRoute)
            }, navigateHomeScreen = {
                navigator.navigateNewGraph(HomeScreenRoute)
            })
        }
        composable<OnboardingStartRoute> {
            catchingUnwrapped { KeystoreService.checkKeyMaterialValid() }.onFailure { Napier.d(it) { "Deleted old Key" } }
            OnboardingStartView(
                onClickStart = { navigator.navigate(OnboardingInformationRoute) },
                onClickLogo = onClickLogo,
                modifier = Modifier.testTag(OnboardingWrapperTestTags.onboardingStartScreen)
            )
        }
        composable<OnboardingInformationRoute> {
            OnboardingInformationView(
                onClickContinue = {
                    settingsRepository.set(isConditionsAccepted = true)
                    navigator.navigateNewGraph(InitializationRoute)
                }, onClickLogo = onClickLogo
            )
        }
        composable<HomeScreenRoute> {
            CredentialsView(
                navigateToAddCredentialsPage = {
                    navigator.navigate(AddCredentialRoute)
                },
                navigateToQrAddCredentialsPage = {
                    navigator.navigate(QrCodeScannerRoute(QrCodeScannerMode.PROVISIONING))
                },
                navigateToCredentialDetailsPage = {
                    navigator.navigate(CredentialDetailsRoute(it))
                },
                onClickLogo = onClickLogo,
                onClickSettings = { navigator.navigate(SettingsRoute) },
                bottomBar = {
                    BottomBar(
                        navigate = navigator::navigate,
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
                    navigator.navigate(QrCodeScannerRoute(QrCodeScannerMode.AUTHENTICATION))
                },
                onNavigateToProximityHolderView = { navigator.navigate(ProximityHolderRoute) },
                onClickLogo = onClickLogo,
                onClickSettings = { navigator.navigate(SettingsRoute) },
                bottomBar = {
                    BottomBar(
                        navigate = navigator::navigate, selected = NavigationData.PRESENT_DATA_SCREEN
                    )
                },
            )
        }

        composable<ProximityHolderRoute> {
            HolderView(
                navigateUp = { navigator.navigate(PresentDataRoute) },
                onClickLogo = onClickLogo,
                onClickSettings = { navigator.navigate(SettingsRoute) },
                onNavigateToPresentmentScreen = {
                    intentState.presentationStateModel.value = it
                    navigator.navigate(LocalPresentationAuthenticationConsentRoute("QR"))
                },
                bottomBar = {
                    BottomBar(
                        navigate = navigator::navigate,
                        selected = NavigationData.PRESENT_DATA_SCREEN
                    )
                },
                onError = onError,
                koinScope = koinScope
            )
        }

        composable<ProximityVerifierRoute> {
            VerifierView(
                navigateUp = { navigator.navigateBack() },
                onClickLogo = onClickLogo,
                onClickSettings = { navigator.navigate(SettingsRoute) },
                onError = onError,
                bottomBar = {
                    BottomBar(
                        navigate = navigator::navigate,
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
                    navigator.navigate(SettingsRoute)
                },
                koinScope = koinScope,
                onNavigateUp = navigator::invocationAwareBack,
                navigateUpIsClose = true,
            )
        }

        composable<DCAPIPresentationViewRoute> {
            DCAPIPresentationGraphView(
                onError = onError,
                onClickLogo = onClickLogo,
                onClickSettings = {
                    navigator.navigate(SettingsRoute)
                },
                koinScope = koinScope,
                onNavigateUp = navigator::invocationAwareBack,
            )
        }

        composable<LocalPresentationAuthenticationConsentRoute> { backStackEntry ->
            val vm = remember {
                try {
                    intentState.presentationStateModel.value?.let {
                        PresentationViewModel(
                            presentationStateModel = it,
                            navigateUp = { navigator.navigateBack() },
                            onAuthenticationSuccess = { },
                            navigateToHomeScreen = { navigator.returnToHome() },
                            walletMain = walletMain,
                            onClickLogo = onClickLogo,
                            onClickSettings = { navigator.navigate(SettingsRoute) })
                    } ?: throw IllegalStateException("No presentation view model set")
                } catch (e: Throwable) {
                    navigator.returnToHome()
                    walletMain.errorService.emit(e)
                    null
                }
            }

            if (vm != null) {
                Napier.d("Showing presentation view")
                PresentationView(
                    vm,
                    onPresentmentComplete = {
                        navigator.returnToHome()
                    },
                    coroutineScope = walletMain.scope,
                    walletMain.snackbarService,
                    onError = { e ->
                        navigator.returnToHome()
                        walletMain.errorService.emit(e)
                    }
                )
            }
        }

        composable<AuthenticationSuccessRoute> { backStackEntry ->
            AuthenticationSuccessView(
                navigateUp = navigator::invocationAwareBack,
                onClickLogo = onClickLogo,
                onClickSettings = { navigator.navigate(SettingsRoute) },
                navigateUpIsClose = true,
            )
        }

        composable<AddCredentialRoute> {
            SelectIssuingServerView(
                navigateUp = navigator::navigateBack,
                onClickLogo = onClickLogo,
                onClickSettings = { navigator.navigate(SettingsRoute) },
                onNavigateToLoadCredentialRoute = { host ->
                    navigator.navigate(LoadCredentialRoute(host))
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
                        navigateUp = navigator::navigateBack,
                        hostString = backStackEntry.toRoute<LoadCredentialRoute>().host,
                        onSubmit = { credentialIdentifierInfo, _, _ ->
                            navigator.returnToHome()
                            walletMain.scope.launch {
                                walletMain.startProvisioning(
                                    host = backStackEntry.toRoute<LoadCredentialRoute>().host,
                                    credentialIdentifierInfo = credentialIdentifierInfo,
                                ) {}
                            }
                        },
                        onClickLogo = onClickLogo,
                        onClickSettings = { navigator.navigate(SettingsRoute) })
                }.onSuccess { vm = it }
                 .onFailure {
                    navigator.returnToHome()
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
                        navigateUp = navigator::navigateBack,
                        url = backStackEntry.toRoute<AddCredentialWithLinkRoute>().uri,
                        onSubmit = { credentialIdentifierInfo, transactionCode, offer ->
                            navigator.returnToHome()
                            navigator.navigate(LoadingRoute)
                            walletMain.scope.launch {
                                try {
                                    walletMain.provisioningService.loadCredentialWithOffer(
                                        credentialOffer = offer!!,
                                        credentialIdentifierInfo = credentialIdentifierInfo,
                                        transactionCode = transactionCode?.ifEmpty { null }
                                            ?.ifBlank { null },
                                    )
                                    navigator.returnToHome()
                                } catch (e: Throwable) {
                                    navigator.returnToHome()
                                    walletMain.errorService.emit(e)
                                }
                            }
                        },
                        onClickLogo = onClickLogo,
                        onClickSettings = { navigator.navigate(SettingsRoute) }
                    )
                }.onSuccess { vm = it }
                 .onFailure {
                    navigator.returnToHome()
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
                        navigateUp = navigator::navigateBack,
                        offer = offer,
                        onSubmit = { credentialIdentifierInfo, transactionCode, _ ->
                            navigator.returnToHome()
                            navigator.navigate(LoadingRoute)
                            walletMain.scope.launch {
                                try {
                                    walletMain.provisioningService.loadCredentialWithOffer(
                                        credentialOffer = offer,
                                        credentialIdentifierInfo = credentialIdentifierInfo,
                                        transactionCode = transactionCode?.ifEmpty { null }
                                            ?.ifBlank { null },
                                    )
                                    navigator.returnToHome()
                                } catch (e: Throwable) {
                                    navigator.returnToHome()
                                    walletMain.errorService.emit(e)
                                }
                            }
                        },
                        onClickLogo = onClickLogo,
                        onClickSettings = { navigator.navigate(SettingsRoute) }
                    )
                }.onSuccess { vm = it }
                 .onFailure {
                    navigator.returnToHome()
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
                        navigator.navigate(LoadingRoute)
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
                                    navigator.navigate(AddCredentialDcApiSuccessRoute)
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
                        navigateUp = navigator::invocationAwareBack,
                        offer = offer,
                        onSubmit = onSubmit,
                        onClickLogo = onClickLogo,
                        onClickSettings = { navigator.navigate(SettingsRoute) }
                    ).also { dcapiVm = it }
                }.onSuccess { vm = it }
                 .onFailure {
                    val wrapped = ErrorHandlingOverrideException(
                        resetStackOverride = navigator::invocationAwareBack,
                        actionDescriptionOverride = Res.string.info_text_error_action_return_to_invoker,
                        onAcknowledge = {
                            if (walletMain.platformAdapter.hasPendingDCAPIIssuingRequest()) {
                                val response = vckJsonSerializer.encodeToString(
                                    DigitalCredentialOfferReturn.error(status = "offer_declined")
                                )
                                walletMain.platformAdapter.prepareDCAPIIssuingResponse(response, false)
                            }
                            navigator.invocationAwareBack()
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
                navigator.invocationAwareBack()
            }

            val backState = rememberNavigationEventState(NavigationEventInfo.None)
            NavigationBackHandler(state = backState, onBackCompleted = onAcknowledge)

            CredentialAddedView(
                onAutoDismiss = onAcknowledge,
                onClickButton = onAcknowledge,
                onClickLogo = onClickLogo,
                onClickSettings = { navigator.navigate(SettingsRoute) }
            )
        }

        composable<CredentialDetailsRoute> { backStackEntry ->
            CredentialDetailsView(vm = remember {
                CredentialDetailsViewModel(
                    storeEntryId = backStackEntry.toRoute<CredentialDetailsRoute>().storeEntryId,
                    navigateUp = navigator::navigateBack,
                    walletMain = walletMain,
                    onClickLogo = onClickLogo,
                    onClickSettings = { navigator.navigate(SettingsRoute) })
            })
        }

        composable<SettingsRoute> { backStackEntry ->
            SettingsView(
                buildType = walletMain.buildContext.buildType,
                version = walletMain.buildContext.versionName,
                onClickShareLogFile = {
                    navigator.navigate(LogRoute)
                },
                onClickLogo = onClickLogo,
                onClickSettings = { navigator.returnToHome() },
                onClickBack = navigator::navigateBack,
                onClickFAQs = null,
                onClickDataProtectionPolicy = null,
                onClickLicenses = null,
                onReset = { navigator.navigateNewGraph(InitializationRoute) },
                koinScope = koinScope
            )
        }

        composable<LogRoute> { backStackEntry ->
            LogView(vm = remember {
                LogViewModel(
                    navigateUp = navigator::navigateBack,
                    walletMain = walletMain,
                    onClickLogo = onClickLogo,
                    onClickSettings = { navigator.navigate(SettingsRoute) })
            })
        }

        composable<ErrorRoute> { backStackEntry ->
            walletMain.errorService.error.collectAsState(null).value?.let {
                catchingUnwrapped {
                    val throwable = if (navigator.shouldFinishToCaller()) {
                        val existingOverride = it.throwable as? ErrorHandlingOverrideException
                        if (existingOverride?.hasUiOverride == true) {
                            existingOverride
                        } else {
                            ErrorHandlingOverrideException(
                                resetStackOverride = navigator::invocationAwareBack,
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
                        resetStack = { navigator.returnToHome() },
                        resetApp = {
                            walletMain.scope.launch {
                                walletMain.resetApp()
                                val resetMessage =
                                    getString(Res.string.snackbar_reset_app_successfully)
                                walletMain.snackbarService.showSnackbar(resetMessage)
                                navigator.popBackStack(InitializationRoute)
                            }
                        },
                        throwable = throwable,
                        onClickLogo = onClickLogo,
                        onClickSettings = { navigator.navigate(SettingsRoute) })
                }.onSuccess {
                    ErrorView(remember { it })
                }.onFailure {
                    navigator.returnToHome()
                }
            }
        }

        composable<LoadingRoute> { backStackEntry ->
            LoadingView()
        }

        composable<SigningQtspSelectionRoute> { backStackEntry ->
            SigningQtspSelectionView(vm = remember {
                SigningQtspSelectionViewModel(
                    navigateUp = navigator::navigateBack,
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
                    onClickSettings = { navigator.navigate(SettingsRoute) },
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
                        navigator.navigateBack()
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
                        navigator.navigateBack()
                        navigator.navigate(route)
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
                        navigator.navigateBack()
                        navigator.navigate(route)
                    },
                    onFailure = { e ->
                        val wrapped = ErrorHandlingOverrideException(
                            resetStackOverride = navigator::invocationAwareBack,
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
                        navigator.navigateNewGraph(route)
                    },
                    onFailure = { e ->
                        val overrideException = ErrorHandlingOverrideException(
                            resetStackOverride = navigator::invocationAwareBack,
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
                        navigator.navigateBack()
                        navigator.navigate(route)
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
                        navigator.returnToHome()
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
                            navigator.navigateBack()
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
                        navigator.returnToHome()
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
                            navigator.navigateBack()
                            navigator.navigate(
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
                onNavigateUp = navigator::navigateBack,
                onClickLogo = onClickLogo,
                onClickSettings = { navigator.navigate(SettingsRoute) },
                onNavigateToRoute = {
                    navigator.navigateBack()
                    navigator.navigate(it)
                },
                onError = {
                    walletMain.errorService.emit(it)
                },
            )
        }
        composable<CapabilitiesRoute> { backStackEntry ->
            backStackEntry.toRoute<CapabilitiesRoute>().prerequisites.let { prerequisites ->
                // Always call NavigationBackHandler unconditionally; use the callback to skip when CRYPTO.
                // CRYPTO prerequisite must not be dismissible via back — user must complete setup.
                val backState = rememberNavigationEventState(NavigationEventInfo.None)
                NavigationBackHandler(state = backState, isBackEnabled = true) {
                    if (!prerequisites.contains(CRYPTO)) {
                        navigator.navigateBack()
                    }
                }
                CapabilityView(
                    koinScope = koinScope,
                    onClickLogo = onClickLogo,
                    onClickSettings = { navigator.navigate(SettingsRoute) },
                    onContinue = {
                        navigator.navigatePending()
                    },
                    onNavigateUp = {
                        navigator.navigateBack()
                    },
                    prerequisites = prerequisites,
                )
            }
        }
    }
}