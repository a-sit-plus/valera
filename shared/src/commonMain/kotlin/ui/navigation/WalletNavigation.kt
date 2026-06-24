package ui.navigation

import AppTestTags
import ErrorHandlingOverrideException
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import at.asitplus.valera.resources.refresh_snackbar_action
import at.asitplus.valera.resources.refresh_snackbar_message_multiple
import at.asitplus.valera.resources.refresh_snackbar_message_single
import at.asitplus.valera.resources.snackbar_local_presentment_busy
import at.asitplus.valera.resources.snackbar_local_presentment_cancel_action
import at.asitplus.valera.resources.snackbar_reset_app_successfully
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.uiLabelNonCompose
import at.asitplus.wallet.app.common.ErrorService
import at.asitplus.wallet.app.common.IntentState
import at.asitplus.wallet.app.common.KeystoreService
import at.asitplus.wallet.app.common.LoadingMessageKey
import at.asitplus.wallet.app.common.SnackbarService
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.decodeImage
import at.asitplus.wallet.app.common.data.SettingsRepository
import at.asitplus.wallet.app.common.presentation.LocalPresentmentSessionCoordinator
import at.asitplus.wallet.app.common.presentation.NfcDispatchSuppressionMode
import at.asitplus.wallet.app.common.presentation.NfcTransferState
import at.asitplus.wallet.app.common.presentation.PresentmentCanceled
import at.asitplus.wallet.app.common.domain.platform.UrlOpener
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import io.github.aakira.napier.Napier
import io.ktor.http.URLBuilder
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.compose.koinInject
import org.koin.core.scope.Scope
import ui.composables.BottomBar
import ui.composables.credentials.CredentialCard
import ui.composables.NavigationData
import ui.navigation.routes.*
import ui.navigation.routes.RoutePrerequisites.CRYPTO
import ui.presentation.DCAPIPresentationGraphView
import ui.presentation.DefaultPresentationGraphView
import ui.viewmodels.*
import ui.viewmodels.authentication.PresentationStateModel
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
    localPresentmentSessionCoordinator: LocalPresentmentSessionCoordinator = koinInject(scope = koinScope),
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
            // Keep the snackbar clear of the system navigation bar so it does not sit at the very
            // bottom edge on top of in-screen bottom action bars.
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding(),
            )
        }, modifier = Modifier.testTag(AppTestTags.rootScaffold)
    ) { _ ->
        WalletNavHost(
            navController = navController,
            startDestination = startDestination,
            navigator = navigator,
            onClickLogo = onClickLogo,
            onError = { e ->
                navigator.popToInvoker()
                errorService.emit(e)
            },
            koinScope = koinScope,
            snackbarHostState = snackbarHostState,
            intentState = intentState,
        )
    }

    LaunchedEffect(koinScope) {
        // Emit unconditionally so errors emitted before the home screen appears
        // (e.g. during InitializationView / onboarding) are not suppressed by
        // the appReady gate in the error and appLink collectors below.
        walletMain.scope.launch {
            Napier.d("WalletNavigation appReady emit")
            walletMain.appReady.emit(true)
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
            localPresentmentSessionCoordinator.busySessionEvents.collect { event ->
                snackbarService.showSnackbar(
                    text = getString(Res.string.snackbar_local_presentment_busy),
                    actionLabel = getString(Res.string.snackbar_local_presentment_cancel_action),
                    duration = SnackbarDuration.Indefinite,
                ) {
                    localPresentmentSessionCoordinator.cancelSession(
                        sessionId = event.sessionId,
                        reason = "busy-snackbar-cancel",
                    )
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
    snackbarHostState: SnackbarHostState,
    walletMain: WalletMain = koinInject(scope = koinScope),
    settingsRepository: SettingsRepository = koinInject(scope = koinScope),
    localPresentmentSessionCoordinator: LocalPresentmentSessionCoordinator = koinInject(scope = koinScope),
    intentState: IntentState,
) {

    val resetNavigationScope = rememberCoroutineScope()
    val items by walletMain.credentialValidityService.refreshItems.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val isOnRefreshCenter = backStackEntry?.destination?.hasRoute<RefreshCenterRoute>() == true

    // Suppress credential-refresh snackbars while the user is in an authentication/presentation
    // flow. Those screens place primary action buttons (Continue, Open URL, …) at the bottom of
    // the screen, exactly where the snackbar floats — so the snackbar would overlay them and
    // intercept taps meant for the action button (regression introduced with #452).
    val isInAuthenticationFlow = backStackEntry?.destination?.let { destination ->
        destination.hasRoute<AuthenticationViewRoute>() ||
                destination.hasRoute<DCAPIPresentationViewRoute>() ||
                destination.hasRoute<LocalPresentationAuthenticationConsentRoute>() ||
                destination.hasRoute<AuthenticationSuccessRoute>() ||
                destination.hasRoute<PresentDataRoute>()
    } == true
    val suppressRefreshSnackbar = isOnRefreshCenter || isInAuthenticationFlow

    var processedItemIds by remember { mutableStateOf(setOf<Long>()) }
    var hasNavigatedToCenter by remember { mutableStateOf(false) }

    LaunchedEffect(items.isEmpty()) {
        if (items.isEmpty()) {
            processedItemIds = emptySet()
            hasNavigatedToCenter = false
        }
    }

    val singleRefreshItem = if (!suppressRefreshSnackbar && items.size == 1) items.first() else null
    LaunchedEffect(singleRefreshItem?.storeEntryId) {
        val item = singleRefreshItem ?: return@LaunchedEffect
        if (processedItemIds.contains(item.storeEntryId)) return@LaunchedEffect
        processedItemIds = processedItemIds + item.storeEntryId
        val result = snackbarHostState.showSnackbar(
            message = getString(Res.string.refresh_snackbar_message_single, item.entry.scheme.uiLabelNonCompose()),
            actionLabel = getString(Res.string.refresh_snackbar_action),
            withDismissAction = true,
            duration = SnackbarDuration.Long,
        )
        when (result) {
            SnackbarResult.ActionPerformed -> {
                walletMain.credentialValidityService.refreshSingleWithStatus(item)
                navController.navigate(RefreshCenterRoute) { launchSingleTop = true }
            }

            SnackbarResult.Dismissed -> walletMain.credentialValidityService.removeRefreshRequest(item)
        }
    }

    val shouldShowMultipleSnackbar = !suppressRefreshSnackbar && items.size > 1 && !hasNavigatedToCenter
    LaunchedEffect(shouldShowMultipleSnackbar) {
        if (!shouldShowMultipleSnackbar) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = getString(Res.string.refresh_snackbar_message_multiple),
            actionLabel = getString(Res.string.refresh_snackbar_action),
            withDismissAction = true,
            duration = SnackbarDuration.Long,
        )
        when (result) {
            SnackbarResult.ActionPerformed -> {
                hasNavigatedToCenter = true
                navController.navigate(RefreshCenterRoute) { launchSingleTop = true }
            }

            SnackbarResult.Dismissed -> walletMain.credentialValidityService.clearAllRefreshRequests()
        }
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
                    navigator.navigateReplacingCurrent(LocalPresentationAuthenticationConsentRoute)
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

        composable<LocalPresentationAuthenticationConsentRoute> { backStackEntry ->
            val activeSession = remember {
                localPresentmentSessionCoordinator.activeSession()?.also { session ->
                    localPresentmentSessionCoordinator.markUiAttached(session.sessionId)
                }
            }
            val vm = remember {
                try {
                    activeSession?.presentationStateModel?.let {
                        PresentationViewModel(
                            presentationStateModel = it,
                            navigateUp = { navigator.navigateBack() },
                            onAuthenticationSuccess = { },
                            navigateToHomeScreen = {
                                // Notify the verifier that we can't fulfil the request by
                                // sending a session-termination message before navigating away.
                                // dismiss(CLICK) sends the termination, completes the session
                                // with PresentmentCanceled, and lets onError → popToInvoker()
                                // handle the navigation. Fallback to direct navigation if no
                                // active session model is available.
                                activeSession?.presentationStateModel
                                    ?.dismiss(PresentationStateModel.DismissType.CLICK)
                                    ?: navigator.popToInvoker()
                            },
                            walletMain = walletMain,
                            onClickLogo = onClickLogo)
                    } ?: throw IllegalStateException("No presentation view model set")
                } catch (e: Throwable) {
                    navigator.popToInvoker()
                    walletMain.errorService.emit(e)
                    null
                }
            }

            if (vm != null) {
                Napier.d("Showing presentation view")
                PresentationView(
                    vm,
                    onPresentmentComplete = {
                        activeSession?.let { session ->
                            localPresentmentSessionCoordinator.finishSession(
                                session.sessionId,
                                "wallet-presentment-complete"
                            )
                        }
                        intentState.presentationStateModel.value = null
                        intentState.presentationStateModelProvider = null
                        navigator.popToInvoker()
                    },
                    coroutineScope = walletMain.scope,
                    walletMain.snackbarService,
                    onError = { e ->
                        activeSession?.let { session ->
                            localPresentmentSessionCoordinator.finishSession(
                                session.sessionId,
                                if (e is PresentmentCanceled) "wallet-presentment-canceled" else "wallet-presentment-error"
                            )
                        }
                        intentState.presentationStateModel.value = null
                        intentState.presentationStateModelProvider = null
                        navigator.popToInvoker()
                        if (e !is PresentmentCanceled) {
                            walletMain.errorService.emit(e)
                        }
                    }
                )
            }
        }

        composable<AddCredentialRoute> {
            SelectIssuingServerView(
                navigateUp = navigator::navigateBack,
                onClickLogo = onClickLogo,
                onNavigateToLoadCredentialRoute = { host ->
                    navigator.navigate(LoadCredentialRoute(host))
                },
                koinScope = koinScope
            )
        }

        composable<LoadCredentialRoute> { backStackEntry ->
            var vm by remember { mutableStateOf<LoadCredentialViewModel?>(null) }
            LaunchedEffect(Unit) {
                catchingUnwrapped {
                    LoadCredentialViewModel.init(
                        walletMain = walletMain,
                        navigateUp = navigator::navigateBack,
                        hostString = backStackEntry.toRoute<LoadCredentialRoute>().host,
                        onSubmit = { credentialIdentifierInfo, _, _ ->
                            navigator.popToInvoker()
                            walletMain.scope.launch {
                                walletMain.startProvisioning(
                                    host = backStackEntry.toRoute<LoadCredentialRoute>().host,
                                    credentialIdentifierInfo = credentialIdentifierInfo,
                                ) {}
                            }
                        },
                        onClickLogo = onClickLogo,
                        onProgress = walletMain.loadingStatusService::set,
                    )
                }.onSuccess {
                    walletMain.loadingStatusService.clear()
                    vm = it
                }
                    .onFailure {
                        walletMain.loadingStatusService.clear()
                        navigator.popToInvoker()
                        walletMain.errorService.emit(it)
                    }
            }
            val loadingMessage by walletMain.loadingStatusService.message.collectAsState()
            vm?.let { LoadCredentialView(it) } ?: LoadingView(
                loadingMessageString(loadingMessage ?: LoadingMessageKey.IssuerMetadata)
            )
        }

        sharedFlowDestinations(
            flow = SharedDestinationFlow.Wallet,
            navigator = navigator,
            onClickLogo = onClickLogo,
            onError = onError,
            koinScope = koinScope,
            walletMain = walletMain,
            localPresentmentSessionCoordinator = localPresentmentSessionCoordinator,
            intentState = intentState,
        )

        composable<SettingsRoute> { backStackEntry ->
            SettingsView(
                buildType = walletMain.buildContext.buildType,
                version = walletMain.buildContext.versionName,
                onClickShareLogFile = {
                    navigator.navigate(LogRoute)
                },
                onClickLogo = onClickLogo,
                onClickBack = navigator::navigateBack,
                onClickFAQs = null,
                onClickDataProtectionPolicy = null,
                onClickLicenses = null,
                onClickAttestation = { navigator.navigate(AttestationSettingsRoute) },
                onReset = { navigator.navigateNewGraph(InitializationRoute) },
                koinScope = koinScope
            )
        }

        composable<ErrorRoute> { backStackEntry ->
            DisposableEffect(Unit) {
                NfcTransferState.nfcDataTransferActive.value = false
                NfcTransferState.holderNfcDataTransferActive.value = false
                NfcTransferState.verifierNfcReaderModeActive.value = false
                NfcTransferState.verifierNfcTransferActive.value = false
                NfcTransferState.verifierNfcTagDispatchSuppressed.value = NfcDispatchSuppressionMode.DISABLED
                onDispose {
                    NfcTransferState.verifierNfcTagDispatchSuppressed.value = NfcDispatchSuppressionMode.NONE
                }
            }
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
                        resetStack = { navigator.popToInvoker() },
                        resetApp = {
                            resetNavigationScope.launch {
                                walletMain.resetApp()
                                navigator.navigateNewGraph(InitializationRoute)
                                val resetMessage =
                                    getString(Res.string.snackbar_reset_app_successfully)
                                walletMain.snackbarService.showSnackbar(resetMessage)
                            }
                        },
                        throwable = throwable,
                        onClickLogo = onClickLogo,
                        onClickSettings = { navigator.navigate(SettingsRoute) })
                }.onSuccess {
                    ErrorView(remember { it })
                }.onFailure {
                    navigator.popToInvoker()
                }
            }
        }

        composable<SigningServiceIntentRoute> { backStackEntry ->
            SigningServiceIntentView(remember {
                SigningServiceIntentViewModel(
                    walletMain = walletMain,
                    uri = backStackEntry.toRoute<SigningServiceIntentRoute>().uri,
                    onSuccess = {
                        navigator.popToInvoker()
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
                        navigator.popToInvoker()
                    },
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
    }
}
