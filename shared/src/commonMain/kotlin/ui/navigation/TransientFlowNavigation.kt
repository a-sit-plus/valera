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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
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
import at.asitplus.wallet.app.common.decodeImage
import at.asitplus.wallet.app.common.ErrorService
import at.asitplus.wallet.app.common.IntentState
import at.asitplus.wallet.app.common.SnackbarService
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.domain.platform.UrlOpener
import at.asitplus.wallet.lib.data.vckJsonSerializer
import io.github.aakira.napier.Napier
import io.ktor.http.URLBuilder
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
    urlOpener: UrlOpener = koinInject(scope = koinScope),
) {
    val navController: NavHostController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val initialLink = remember {
        // appLink is always set alongside DC API data by registerDcapiPreRequest,
        // registerDcapiInvocation, and applyPendingState, so reading appLink alone is sufficient.
        intentState.appLink.value.also { link ->
            if (link != null) intentState.appLink.value = null
        }
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

    val startDestination = remember(initialLink) {
        when (initialLink) {
            null -> LoadingRoute
            IntentService.IOS_DC_API_PRE_REQUEST -> IosDcApiPreRequestRoute
            else -> {
                try {
                    intentService.handleIntent(initialLink)
                } catch (e: Throwable) {
                    Napier.e("TransientFlowNavigation could not parse initialLink", e)
                    LoadingRoute
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { _ ->
        TransientFlowNavHost(
            navController = navController,
            startDestination = startDestination,
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
            errorService.error.combineTransform(walletMain.appReady) { error, ready ->
                if (ready == true) emit(error)
            }.collect {
                navigator.navigate(ErrorRoute)
            }
        }
    }
}

// Shared implementation for AddCredentialWithLinkRoute and ProvisioningStartIntentRoute,
// which differ only in the route type they extract the URI from.
@Composable
private fun LoadCredentialFromUrlContent(
    uri: String,
    navigator: WalletNavigationController,
    walletMain: WalletMain,
    onClickLogo: () -> Unit,
) {
    var vm by remember { mutableStateOf<LoadCredentialViewModel?>(null) }
    LaunchedEffect(Unit) {
        runCatching {
            LoadCredentialViewModel.init(
                walletMain = walletMain,
                navigateUp = navigator::navigateBack,
                url = uri,
                onSubmit = { credentialIdentifierInfo, transactionCode, offer ->
                    navigator.navigate(LoadingRoute)
                    walletMain.scope.launch {
                        try {
                            val issuanceResult = walletMain.provisioningService.loadCredentialWithOffer(
                                credentialOffer = offer!!,
                                credentialIdentifierInfo = credentialIdentifierInfo,
                                transactionCode = transactionCode?.ifEmpty { null }?.ifBlank { null },
                            )
                            if (issuanceResult.credentialIssuanceResult is at.asitplus.wallet.lib.ktor.openid.CredentialIssuanceResult.Success) {
                                navigator.navigateNewGraph(
                                    TransientFlowIssuingResultRoute(issuanceResult.storedEntryIds.firstOrNull())
                                )
                            }
                        } catch (e: Throwable) {
                            walletMain.errorService.emit(e)
                        }
                    }
                },
                onClickLogo = onClickLogo,
                onClickSettings = { navigator.navigate(SettingsRoute) }
            )
        }.onSuccess { vm = it }
         .onFailure {
            walletMain.errorService.emit(it)
        }
    }
    vm?.let { LoadCredentialView(it) } ?: LoadingView()
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
    intentState: IntentState,
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
                        navigator.navigateNewGraph(route)
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
                        navigator.navigateNewGraph(route)
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

        composable<IosDcApiPreRequestRoute> {
            IosDcApiPreRequestView(
                intentState = intentState,
                onError = onError,
            )
        }

        composable<DCAPIIssuingIntentRoute> { backStackEntry ->
            DCAPIIssuingIntentView(remember {
                DCAPIIssuingIntentViewModel(
                    walletMain = walletMain,
                    uri = backStackEntry.toRoute<DCAPIIssuingIntentRoute>().uri,
                    onSuccess = { route ->
                        Napier.d("valid DCAPI creation request")
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
                        navigator.navigateNewGraph(route)
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
                onClickSettings = { navigator.navigate(SettingsRoute) },
                koinScope = koinScope,
                onNavigateUp = navigator::invocationAwareBack,
                navigateUpIsClose = true,
            )
        }

        composable<DCAPIPresentationViewRoute> {
            DCAPIPresentationGraphView(
                onError = onError,
                onClickLogo = onClickLogo,
                onClickSettings = { navigator.navigate(SettingsRoute) },
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
                            navigateUp = { navigator.returnToHome() },
                            onAuthenticationSuccess = { },
                            navigateToHomeScreen = { navigator.returnToHome() },
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
                    onPresentmentComplete = { navigator.returnToHome() },
                    coroutineScope = walletMain.scope,
                    walletMain.snackbarService,
                    onError = { e ->
                        walletMain.errorService.emit(e)
                    }
                )
            }
        }

        composable<AuthenticationSuccessRoute> {
            AuthenticationSuccessView(
                navigateUp = navigator::invocationAwareBack,
                onClickLogo = onClickLogo,
                onClickSettings = { navigator.navigate(SettingsRoute) },
                navigateUpIsClose = true,
            )
        }

        composable<AddCredentialWithLinkRoute> { backStackEntry ->
            LoadCredentialFromUrlContent(
                uri = backStackEntry.toRoute<AddCredentialWithLinkRoute>().uri,
                navigator = navigator,
                walletMain = walletMain,
                onClickLogo = onClickLogo,
            )
        }

        composable<ProvisioningStartIntentRoute> { backStackEntry ->
            LoadCredentialFromUrlContent(
                uri = backStackEntry.toRoute<ProvisioningStartIntentRoute>().uri,
                navigator = navigator,
                walletMain = walletMain,
                onClickLogo = onClickLogo,
            )
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
                                        transactionCode = transactionCode?.ifEmpty { null }?.ifBlank { null },
                                    )
                                    navigator.returnToHome()
                                } catch (e: Throwable) {
                                    walletMain.errorService.emit(e)
                                }
                            }
                        },
                        onClickLogo = onClickLogo,
                        onClickSettings = { navigator.navigate(SettingsRoute) }
                    )
                }.onSuccess { vm = it }
                 .onFailure {
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
                                    transactionCode = transactionCode?.ifEmpty { null }?.ifBlank { null },
                                    authorizationServerMetadata = offer.authorizationServerMetadata
                                )
                                if (issuanceResult.credentialIssuanceResult is at.asitplus.wallet.lib.ktor.openid.CredentialIssuanceResult.Success) {
                                    navigator.navigate(
                                        TransientFlowIssuingResultRoute(issuanceResult.storedEntryIds.firstOrNull())
                                    )
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

        composable<ProvisioningAuthRequestIntentRoute> { backStackEntry ->
            var vm by remember { mutableStateOf<LoadCredentialViewModel?>(null) }
            val route = backStackEntry.toRoute<ProvisioningAuthRequestIntentRoute>()
            LaunchedEffect(Unit) {
                runCatching {
                    val credentialIssuer = URLBuilder(route.uri).parameters["credential_issuer"]
                        ?: throw IllegalArgumentException("Missing credential_issuer in issuing authorization request")
                    LoadCredentialViewModel.init(
                        walletMain = walletMain,
                        navigateUp = navigator::navigateBack,
                        hostString = credentialIssuer,
                        onSubmit = { credentialIdentifierInfo, _, _ ->
                            walletMain.scope.launch {
                                walletMain.startProvisioning(
                                    host = credentialIssuer,
                                    credentialIdentifierInfo = credentialIdentifierInfo,
                                ) {}
                            }
                        },
                        onClickLogo = onClickLogo,
                        onClickSettings = { navigator.navigate(SettingsRoute) }
                    )
                }.onSuccess { vm = it }
                    .onFailure {
                        walletMain.errorService.emit(it)
                    }
            }
            vm?.let { LoadCredentialView(it) } ?: LoadingView()
        }

        composable<TransientFlowIssuingResultRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<TransientFlowIssuingResultRoute>()
            var isAutoDismissEnabled by rememberSaveable(route.storeEntryId) { mutableStateOf(true) }
            val detailsStoreEntryId = route.storeEntryId
            val storeEntry = route.storeEntryId?.let { storeEntryId ->
                walletMain.subjectCredentialStore.observeStoreContainer().map { container ->
                    container.credentials.find { it.first == storeEntryId }?.second
                }.collectAsState(null).value
            }
            LaunchedEffect(route.storeEntryId, storeEntry) {
                Napier.d(
                    "TransientFlowIssuingResultRoute render storeEntryId=${route.storeEntryId} " +
                        "resolved=${storeEntry != null} scheme=${storeEntry?.scheme?.schemaUri}"
                )
            }
            val onAcknowledge = {
                if (walletMain.platformAdapter.hasPendingDCAPIIssuingRequest()) {
                    val response = vckJsonSerializer.encodeToString(DigitalCredentialOfferReturn.success())
                    walletMain.platformAdapter.prepareDCAPIIssuingResponse(response, true)
                }
                navigator.returnToHome()
            }

            val backState = rememberNavigationEventState(NavigationEventInfo.None)
            NavigationBackHandler(state = backState, onBackCompleted = onAcknowledge)

            CredentialAddedView(
                onAutoDismiss = onAcknowledge,
                onClickButton = onAcknowledge,
                onClickLogo = onClickLogo,
                onClickSettings = { navigator.navigate(SettingsRoute) },
                isAutoDismissEnabled = isAutoDismissEnabled,
                credentialContent = storeEntry?.let { credential ->
                    {
                        CredentialCard(
                            credential = credential,
                            isTokenStatusEvaluated = true,
                            credentialFreshnessSummaryModel = null,
                            imageDecoder = { image -> walletMain.platformAdapter.decodeImage(image) },
                            onDelete = {},
                            onRefresh = {},
                            onOpenDetails = detailsStoreEntryId?.let { storeEntryId ->
                                {
                                    isAutoDismissEnabled = false
                                    navigator.navigate(CredentialDetailsRoute(storeEntryId))
                                }
                            },
                            showActionMenu = false,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
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

        composable<ProvisioningResumeIntentRoute> { backStackEntry ->
            ProvisioningIntentView(remember {
                ProvisioningIntentViewModel(
                    walletMain = walletMain,
                    uri = backStackEntry.toRoute<ProvisioningResumeIntentRoute>().uri,
                    onSuccess = { route ->
                        navigator.navigateNewGraph(route ?: TransientFlowIssuingResultRoute())
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
                            navigator.navigateNewGraph(
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

        composable<SigningResumeIntentRoute> { backStackEntry ->
            SigningResumeIntentView(remember {
                SigningResumeIntentViewModel(
                    walletMain = walletMain,
                    uri = backStackEntry.toRoute<SigningResumeIntentRoute>().uri,
                    onReturnToSigning = { navigator.navigateBack() },
                    onFinish = { navigator.returnToHome() },
                    onFailure = { error ->
                        walletMain.errorService.emit(error)
                    }
                )
            })
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
                        resetStack = { navigator.returnToHome() },
                        resetApp = {
                            walletMain.scope.launch { walletMain.resetApp() }
                            navigator.returnToHome()
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

        composable<LoadingRoute> {
            LoadingView()
        }

        composable<SettingsRoute> {
            SettingsView(
                buildType = walletMain.buildContext.buildType,
                version = walletMain.buildContext.versionName,
                onClickShareLogFile = { navigator.navigate(LogRoute) },
                onClickLogo = onClickLogo,
                onClickSettings = { navigator.returnToHome() },
                onClickBack = navigator::navigateBack,
                onClickFAQs = null,
                onClickDataProtectionPolicy = null,
                onClickLicenses = null,
                onReset = {
                    walletMain.scope.launch { walletMain.resetApp() }
                    navigator.returnToHome()
                },
                koinScope = koinScope
            )
        }

        composable<LogRoute> {
            LogView(vm = remember {
                LogViewModel(
                    navigateUp = navigator::navigateBack,
                    walletMain = walletMain,
                    onClickLogo = onClickLogo,
                    onClickSettings = { navigator.navigate(SettingsRoute) })
            })
        }

        composable<CapabilitiesRoute> { backStackEntry ->
            backStackEntry.toRoute<CapabilitiesRoute>().prerequisites.let { prerequisites ->
                // Always call NavigationBackHandler unconditionally; use the callback to skip when CRYPTO.
                // CRYPTO prerequisite must not be dismissible via back — user must complete setup.
                val backState = rememberNavigationEventState(NavigationEventInfo.None)
                NavigationBackHandler(state = backState, isBackEnabled = true) {
                    if (!prerequisites.contains(CRYPTO)) {
                        // TransientFlowNavigationControllerImpl.navigateBack() calls finishApp when exhausted.
                        navigator.navigateBack()
                    }
                }
                CapabilityView(
                    koinScope = koinScope,
                    onClickLogo = onClickLogo,
                    onClickSettings = { navigator.navigate(SettingsRoute) },
                    onContinue = { navigator.navigatePending() },
                    onNavigateUp = { navigator.navigateBack() },
                    prerequisites = prerequisites,
                )
            }
        }
    }
}
