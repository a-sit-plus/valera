package ui.navigation

import ErrorHandlingOverrideException
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import at.asitplus.catchingUnwrapped
import at.asitplus.dcapi.issuance.DigitalCredentialOfferReturn
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.info_text_error_action_return_to_invoker
import at.asitplus.valera.resources.snackbar_local_presentment_busy
import at.asitplus.valera.resources.snackbar_local_presentment_cancel_action
import at.asitplus.wallet.app.common.decodeImage
import at.asitplus.wallet.app.common.ErrorService
import at.asitplus.wallet.app.common.IntentState
import at.asitplus.wallet.app.common.LoadingMessageKey
import at.asitplus.wallet.app.common.SnackbarService
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.presentation.LocalPresentmentSessionCoordinator
import at.asitplus.wallet.app.common.presentation.NfcDispatchSuppressionMode
import at.asitplus.wallet.app.common.presentation.NfcTransferState
import at.asitplus.wallet.app.common.presentation.PresentmentCanceled
import at.asitplus.wallet.app.common.domain.platform.UrlOpener
import at.asitplus.wallet.lib.data.vckJsonSerializer
import io.github.aakira.napier.Napier
import io.ktor.http.URLBuilder
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.compose.koinInject
import org.koin.core.scope.Scope
import ui.composables.credentials.CredentialCard
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
fun TransientFlowNavigation(
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
    val scope = rememberCoroutineScope()

    fun startDestinationFor(link: String?): Route =
        when (link) {
            IntentService.IOS_DC_API_PRE_REQUEST ->
                if (intentState.iosDcApiPreRequestData.value != null) {
                    IosDcApiPreRequestRoute
                } else {
                    LoadingRoute(LoadingMessageKey.IncomingRequest)
                }

            IntentService.IOS_DC_API_CALL ->
                if (intentState.dcapiInvocationData.value != null) {
                    intentService.handleIntent(
                        IntentService.IOS_DC_API_CALL,
                        IntentService.IntentType.DCAPIAuthorizationIntent
                    )
                } else {
                    LoadingRoute(LoadingMessageKey.IncomingRequest)
                }

            null -> LoadingRoute(LoadingMessageKey.Generic)
            else -> {
                try {
                    intentService.handleIntent(link)
                } catch (e: Throwable) {
                    Napier.e("TransientFlowNavigation could not parse initialLink", e)
                    LoadingRoute(LoadingMessageKey.IncomingRequest)
                }
            }
        }

    val initialLink = remember {
        val link = intentState.appLink.value ?: when {
            intentState.iosDcApiPreRequestData.value != null -> IntentService.IOS_DC_API_PRE_REQUEST
            intentState.dcapiInvocationData.value != null -> IntentService.IOS_DC_API_CALL
            else -> null
        }
        if (intentState.appLink.value == link) {
            intentState.appLink.value = null
        }
        link
    }

    val navigator: WalletNavigationController = remember(navController, scope) {
        TransientFlowNavigationControllerImpl(
            navController = navController,
            scope = scope,
            intentState = intentState,
            capabilitiesService = walletMain.capabilitiesService,
        )
    }

    val onClickLogo = { urlOpener("https://wallet.a-sit.at/") }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { _ ->
        TransientFlowNavHost(
            navController = navController,
            startDestination = startDestinationFor(initialLink),
            navigator = navigator,
            onClickLogo = onClickLogo,
            onError = { e ->
                errorService.emit(e)
            },
            koinScope = koinScope,
            intentState = intentState,
        )
    }

    LaunchedEffect(koinScope) {
        // Reset before subscribing: if appReady already holds true from a previous session
        // (e.g. same WalletMain reused across recompositions), error navigation could fire
        // before the NavHost graph is established for this composition.
        walletMain.appReady.value = false
        walletMain.scope.launch {
            walletMain.appReady.emit(true)
        }
        this.launch {
            intentState.appLink.combineTransform(walletMain.appReady) { link, ready ->
                if (ready != true || link == null) return@combineTransform
                emit(link)
            }.collect { link ->
                catchingUnwrapped {
                    val intentType = intentService.parseUrl(link)
                    val route = intentService.handleIntent(link, intentType)
                    if (intentService.isContinuationIntent(intentType)) {
                        navigator.navigate(route)
                    } else {
                        // Replace LoadingRoute with the real destination, so pressing back
                        // returns to the invoker rather than flashing LoadingView.
                        navigator.navigateNewGraph(route)
                    }
                }.onFailure {
                    errorService.emit(it)
                }
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
                if (ready == true) emit(error)
            }.collect {
                navigator.navigate(ErrorRoute)
            }
        }
    }
}

@ExperimentalMaterial3Api
@Composable
private fun TransientFlowNavHost(
    navController: NavHostController,
    startDestination: Route,
    navigator: WalletNavigationController,
    onClickLogo: () -> Unit,
    onError: (Throwable) -> Unit,
    koinScope: Scope,
    walletMain: WalletMain = koinInject(scope = koinScope),
    localPresentmentSessionCoordinator: LocalPresentmentSessionCoordinator = koinInject(scope = koinScope),
    intentState: IntentState,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
    ) {

        composable<IosDcApiPreRequestRoute> {
            val preRequestData by intentState.iosDcApiPreRequestData.collectAsState()
            val invocationData by intentState.dcapiInvocationData.collectAsState()

            LaunchedEffect(preRequestData, invocationData) {
                if (preRequestData == null && invocationData != null) {
                    navigator.navigateNewGraph(DCAPIAuthorizationIntentRoute(IntentService.IOS_DC_API_CALL))
                }
            }

            IosDcApiPreRequestView(
                intentState = intentState,
                onError = onError,
            )
        }

        sharedFlowDestinations(
            flow = SharedDestinationFlow.Transient,
            navigator = navigator,
            onClickLogo = onClickLogo,
            onError = onError,
            koinScope = koinScope,
            walletMain = walletMain,
            localPresentmentSessionCoordinator = localPresentmentSessionCoordinator,
            intentState = intentState,
        )

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
                            navigateUp = { navigator.popToInvoker() },
                            onAuthenticationSuccess = { },
                            navigateToHomeScreen = { navigator.popToInvoker() },
                            walletMain = walletMain,
                            onClickLogo = onClickLogo,
                            onClickSettings = { navigator.navigate(SettingsRoute) })
                    } ?: throw IllegalStateException("No presentation view model set")
                } catch (e: Throwable) {
                    walletMain.errorService.emit(e)
                    null
                }
            }

            if (vm != null) {
                PresentationView(
                    vm,
                    onPresentmentComplete = {
                        activeSession?.let { session ->
                            localPresentmentSessionCoordinator.finishSession(
                                session.sessionId,
                                "transient-presentment-complete"
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
                                if (e is PresentmentCanceled) "transient-presentment-canceled" else "transient-presentment-error"
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

        composable<ErrorRoute> {
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
                    val existingOverride = it.throwable as? ErrorHandlingOverrideException
                    val throwable = if (existingOverride?.hasUiOverride == true) {
                        existingOverride
                    } else {
                        ErrorHandlingOverrideException(
                            resetStackOverride = navigator::invocationAwareBack,
                            actionDescriptionOverride = Res.string.info_text_error_action_return_to_invoker,
                            onAcknowledge = existingOverride?.onAcknowledge,
                            cause = existingOverride?.cause ?: it.throwable
                        )
                    }
                    ErrorViewModel(
                        clearError = { walletMain.errorService.clear() },
                        resetStack = { navigator.popToInvoker() },
                        resetApp = {
                            walletMain.scope.launch { walletMain.resetApp() }
                            navigator.popToInvoker()
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

        composable<SettingsRoute> {
            SettingsView(
                buildType = walletMain.buildContext.buildType,
                version = walletMain.buildContext.versionName,
                onClickShareLogFile = { navigator.navigate(LogRoute) },
                onClickLogo = onClickLogo,
                onClickSettings = navigator::navigateBack,
                onClickBack = navigator::navigateBack,
                onClickFAQs = null,
                onClickDataProtectionPolicy = null,
                onClickLicenses = null,
                onReset = {
                    walletMain.scope.launch { walletMain.resetApp() }
                    navigator.popToInvoker()
                },
                koinScope = koinScope,
                onClickAttestation = { navigator.navigate(AttestationSettingsRoute) },
            )
        }

    }
}
