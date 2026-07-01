package ui.navigation

import ErrorHandlingOverrideException
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import at.asitplus.catchingUnwrapped
import at.asitplus.dcapi.issuance.DigitalCredentialOfferReturn
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.info_text_error_action_return_to_invoker
import at.asitplus.wallet.app.common.IntentState
import at.asitplus.wallet.app.common.LoadingMessageKey
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.decodeImage
import at.asitplus.wallet.app.common.presentation.LocalPresentmentSessionCoordinator
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.identifier
import at.asitplus.wallet.lib.ktor.openid.CredentialIssuanceResult
import io.github.aakira.napier.Napier
import io.ktor.http.URLBuilder
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.scope.Scope
import ui.composables.credentials.CredentialCard
import ui.navigation.routes.AddCredentialDcApiRoute
import ui.navigation.routes.AddCredentialPreAuthnRoute
import ui.navigation.routes.AddCredentialWithLinkRoute
import ui.navigation.routes.AttestationSettingsRoute
import ui.navigation.routes.AuthenticationSuccessRoute
import ui.navigation.routes.AuthenticationViewRoute
import ui.navigation.routes.AuthorizationIntentRoute
import ui.navigation.routes.CapabilitiesRoute
import ui.navigation.routes.CredentialDetailsRoute
import ui.navigation.routes.DCAPIAuthorizationIntentRoute
import ui.navigation.routes.DCAPIIssuingIntentRoute
import ui.navigation.routes.DCAPIPresentationViewRoute
import ui.navigation.routes.ErrorIntentRoute
import ui.navigation.routes.LoadingRoute
import ui.navigation.routes.LogRoute
import ui.navigation.routes.PresentationIntentRoute
import ui.navigation.routes.ProvisioningAuthRequestIntentRoute
import ui.navigation.routes.ProvisioningResumeIntentRoute
import ui.navigation.routes.ProvisioningStartIntentRoute
import ui.navigation.routes.Route
import ui.navigation.routes.SettingsRoute
import ui.navigation.routes.SigningIntentRoute
import ui.navigation.routes.SigningQtspSelectionRoute
import ui.navigation.routes.SigningResumeIntentRoute
import ui.navigation.routes.TransientFlowIssuingResultRoute
import ui.presentation.DCAPIPresentationGraphView
import ui.presentation.DefaultPresentationGraphView
import ui.viewmodels.AttestationSettingsViewModel
import ui.viewmodels.CredentialDetailsViewModel
import ui.viewmodels.CredentialSelection
import ui.viewmodels.LoadCredentialViewModel
import ui.viewmodels.LogViewModel
import ui.viewmodels.SigningQtspSelectionViewModel
import ui.viewmodels.intents.AuthorizationIntentViewModel
import ui.viewmodels.intents.DCAPIAuthorizationIntentViewModel
import ui.viewmodels.intents.DCAPIIssuingIntentViewModel
import ui.viewmodels.intents.ErrorIntentViewModel
import ui.viewmodels.intents.PresentationIntentViewModel
import ui.viewmodels.intents.ProvisioningIntentViewModel
import ui.viewmodels.intents.SigningIntentViewModel
import ui.viewmodels.intents.SigningResumeIntentViewModel
import ui.views.AttestationSettingsView
import ui.views.CapabilityView
import ui.views.CredentialAddedView
import ui.views.CredentialDetailsView
import ui.views.LoadCredentialView
import ui.views.LoadingView
import ui.views.LogView
import ui.views.SigningQtspSelectionView
import ui.views.authentication.AuthenticationSuccessView
import ui.views.intents.AuthorizationIntentView
import ui.views.intents.DCAPIAuthorizationIntentView
import ui.views.intents.DCAPIIssuingIntentView
import ui.views.intents.ErrorIntentView
import ui.views.intents.PresentationIntentView
import ui.views.intents.ProvisioningIntentView
import ui.views.intents.SigningIntentView
import ui.views.intents.SigningResumeIntentView
import ui.views.loadingMessageString

internal enum class SharedDestinationFlow {
    Wallet,
    Transient,
}

@OptIn(ExperimentalMaterial3Api::class)
internal fun NavGraphBuilder.sharedFlowDestinations(
    flow: SharedDestinationFlow,
    navigator: WalletNavigationController,
    onClickLogo: () -> Unit,
    onError: (Throwable) -> Unit,
    koinScope: Scope,
    walletMain: WalletMain,
    localPresentmentSessionCoordinator: LocalPresentmentSessionCoordinator,
    intentState: IntentState,
) {
    composable<AuthorizationIntentRoute> { backStackEntry ->
        AuthorizationIntentView(remember {
            AuthorizationIntentViewModel(
                walletMain = walletMain,
                uri = backStackEntry.toRoute<AuthorizationIntentRoute>().uri,
                onSuccess = { route ->
                    flow.navigateAfterIntentSuccess(navigator, route)
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
                    Napier.d(
                        when (flow) {
                            SharedDestinationFlow.Wallet -> "valid authentication request"
                            SharedDestinationFlow.Transient -> "valid DCAPI authentication request"
                        }
                    )
                    flow.navigateAfterIntentSuccess(navigator, route)
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
                    Napier.d(
                        when (flow) {
                            SharedDestinationFlow.Wallet -> "valid creation request"
                            SharedDestinationFlow.Transient -> "valid DCAPI creation request"
                        }
                    )
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
                localPresentmentSessionCoordinator = localPresentmentSessionCoordinator,
                intentState = intentState,
                uri = backStackEntry.toRoute<PresentationIntentRoute>().uri,
                onSuccess = { route ->
                    Napier.d("valid presentation request")
                    flow.navigateAfterIntentSuccess(navigator, route)
                },
                onFailure = { error ->
                    walletMain.errorService.emit(error)
                })
        })
    }

    composable<AuthenticationViewRoute> {
        DefaultPresentationGraphView(
            onError = onError,
            onClickLogo = onClickLogo,
            koinScope = koinScope,
            onNavigateUp = navigator::invocationAwareBack,
            navigateUpIsClose = true,
        )
    }

    composable<DCAPIPresentationViewRoute> {
        DCAPIPresentationGraphView(
            onError = onError,
            onClickLogo = onClickLogo,
            koinScope = koinScope,
            onNavigateUp = navigator::invocationAwareBack,
            showStartRoute = flow == SharedDestinationFlow.Wallet,
        )
    }

    composable<AuthenticationSuccessRoute> {
        AuthenticationSuccessView(
            koinScope = koinScope,
            navigateUp = navigator::invocationAwareBack,
            onClickLogo = onClickLogo,
            navigateUpIsClose = true,
        )
    }

    composable<AddCredentialWithLinkRoute> { backStackEntry ->
        LoadCredentialFromUrlContent(
            uri = backStackEntry.toRoute<AddCredentialWithLinkRoute>().uri,
            flow = flow,
            navigator = navigator,
            walletMain = walletMain,
            onClickLogo = onClickLogo,
        )
    }

    composable<ProvisioningStartIntentRoute> { backStackEntry ->
        LoadCredentialFromUrlContent(
            uri = backStackEntry.toRoute<ProvisioningStartIntentRoute>().uri,
            flow = flow,
            navigator = navigator,
            walletMain = walletMain,
            onClickLogo = onClickLogo,
        )
    }

    composable<AddCredentialPreAuthnRoute> { backStackEntry ->
        val offer = backStackEntry.toRoute<AddCredentialPreAuthnRoute>().credentialOffer
        var vm by remember { mutableStateOf<LoadCredentialViewModel?>(null) }
        LaunchedEffect(Unit) {
            catchingUnwrapped {
                LoadCredentialViewModel.init(
                    walletMain = walletMain,
                    navigateUp = navigator::navigateBack,
                    offer = offer,
                    onSubmit = { credentialIdentifierInfo, transactionCode, _ ->
                        navigator.popToInvoker()
                        walletMain.loadingStatusService.set(LoadingMessageKey.IssuingCredential)
                        navigator.navigate(LoadingRoute(LoadingMessageKey.IssuingCredential))
                        walletMain.scope.launch {
                            try {
                                walletMain.provisioningService.loadCredentialWithOffer(
                                    credentialOffer = offer,
                                    credentialIdentifierInfo = credentialIdentifierInfo,
                                    transactionCode = transactionCode?.ifEmpty { null }?.ifBlank { null },
                                    onProgress = walletMain.loadingStatusService::set,
                                )
                                walletMain.loadingStatusService.clear()
                                navigator.popToInvoker()
                            } catch (e: Throwable) {
                                walletMain.loadingStatusService.clear()
                                flow.handleCredentialFlowFailure(navigator, walletMain, e)
                            }
                        }
                    },
                    onClickLogo = onClickLogo,
                    onProgress = walletMain.loadingStatusService::set,
                )
            }.onSuccess {
                walletMain.loadingStatusService.clear()
                vm = it
            }.onFailure {
                walletMain.loadingStatusService.clear()
                flow.handleCredentialFlowFailure(navigator, walletMain, it)
            }
        }
        val loadingMessage by walletMain.loadingStatusService.message.collectAsState()
        vm?.let { LoadCredentialView(it) } ?: LoadingView(
            loadingMessageString(loadingMessage ?: LoadingMessageKey.IssuerMetadata)
        )
    }

    composable<AddCredentialDcApiRoute> { backStackEntry ->
        val offer = backStackEntry.toRoute<AddCredentialDcApiRoute>().credentialOffer
        var vm by remember { mutableStateOf<LoadCredentialViewModel?>(null) }
        LaunchedEffect(Unit) {
            catchingUnwrapped {
                lateinit var dcapiVm: LoadCredentialViewModel
                val onSubmit: CredentialSelection = { credentialIdentifierInfo, transactionCode, _ ->
                    walletMain.loadingStatusService.set(LoadingMessageKey.IssuingCredential)
                    navigator.navigate(LoadingRoute(LoadingMessageKey.IssuingCredential))
                    walletMain.scope.launch {
                        try {
                            val issuanceResult = walletMain.provisioningService.loadCredentialWithOffer(
                                credentialOffer = offer,
                                credentialIdentifierInfo = credentialIdentifierInfo,
                                transactionCode = transactionCode?.ifEmpty { null }?.ifBlank { null },
                                authorizationServerMetadata = offer.authorizationServerMetadata,
                                onProgress = walletMain.loadingStatusService::set,
                            )
                            walletMain.loadingStatusService.clear()
                            if (issuanceResult.credentialIssuanceResult is CredentialIssuanceResult.Success) {
                                navigator.navigate(
                                    TransientFlowIssuingResultRoute(issuanceResult.storedEntryIds.firstOrNull())
                                )
                            } else {
                                dcapiVm.handleDCAPIIssuingResult(false, null)
                            }
                        } catch (e: Throwable) {
                            walletMain.loadingStatusService.clear()
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
                    onProgress = walletMain.loadingStatusService::set,
                ).also { dcapiVm = it }
            }.onSuccess {
                walletMain.loadingStatusService.clear()
                vm = it
            }
                .onFailure {
                    walletMain.loadingStatusService.clear()
                    val wrapped = ErrorHandlingOverrideException(
                        resetStackOverride = navigator::invocationAwareBack,
                        actionDescriptionOverride = Res.string.info_text_error_action_return_to_invoker,
                        onAcknowledge = {
                            if (walletMain.platformAdapter.hasPendingDCAPIIssuingRequest()) {
                                val response = flow.encodeDigitalCredentialOfferReturn(
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
        val loadingMessage by walletMain.loadingStatusService.message.collectAsState()
        vm?.let { LoadCredentialView(it) } ?: LoadingView(
            loadingMessageString(loadingMessage ?: LoadingMessageKey.IssuerMetadata)
        )
    }

    composable<ProvisioningAuthRequestIntentRoute> { backStackEntry ->
        var vm by remember { mutableStateOf<LoadCredentialViewModel?>(null) }
        val route = backStackEntry.toRoute<ProvisioningAuthRequestIntentRoute>()
        LaunchedEffect(Unit) {
            catchingUnwrapped {
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
                    onProgress = walletMain.loadingStatusService::set,
                )
            }.onSuccess {
                walletMain.loadingStatusService.clear()
                vm = it
            }.onFailure {
                walletMain.loadingStatusService.clear()
                flow.handleCredentialFlowFailure(navigator, walletMain, it)
            }
        }
        val loadingMessage by walletMain.loadingStatusService.message.collectAsState()
        vm?.let { LoadCredentialView(it) } ?: LoadingView(
            loadingMessageString(loadingMessage ?: LoadingMessageKey.IssuerMetadata)
        )
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
            val prefix = when (flow) {
                SharedDestinationFlow.Wallet -> "Wallet "
                SharedDestinationFlow.Transient -> ""
            }
            Napier.d(
                "${prefix}TransientFlowIssuingResultRoute render storeEntryId=${route.storeEntryId} " +
                    "resolved=${storeEntry != null} scheme=${storeEntry?.scheme?.identifier}"
            )
        }
        val onAcknowledge = {
            if (walletMain.platformAdapter.hasPendingDCAPIIssuingRequest()) {
                val response = flow.encodeDigitalCredentialOfferReturn(DigitalCredentialOfferReturn.success())
                walletMain.platformAdapter.prepareDCAPIIssuingResponse(response, true)
            }
            navigator.popToInvoker()
        }

        val backState = rememberNavigationEventState(NavigationEventInfo.None)
        NavigationBackHandler(state = backState, onBackCompleted = onAcknowledge)

        CredentialAddedView(
            onAutoDismiss = onAcknowledge,
            onClickButton = onAcknowledge,
            onClickLogo = onClickLogo,
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
                        onOpenDetails = detailsStoreEntryId.let { storeEntryId ->
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
                onClickLogo = onClickLogo)
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
                onFinish = { navigator.popToInvoker() },
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

    composable<LoadingRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<LoadingRoute>()
        val loadingMessage by walletMain.loadingStatusService.message.collectAsState()
        LoadingView(loadingMessageString(loadingMessage ?: route.messageKey))
    }

    composable<LogRoute> {
        LogView(vm = remember {
            LogViewModel(
                navigateUp = navigator::navigateBack,
                walletMain = walletMain,
                onClickLogo = onClickLogo)
        })
    }

    composable<CapabilitiesRoute> { backStackEntry ->
        backStackEntry.toRoute<CapabilitiesRoute>().prerequisites.let { prerequisites ->
            val backState = rememberNavigationEventState(NavigationEventInfo.None)
            NavigationBackHandler(state = backState, isBackEnabled = true) {
                navigator.navigateBack()
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

    composable<AttestationSettingsRoute> {
        AttestationSettingsView(
            onClickLogo = onClickLogo,
            onClickBack = { navigator.navigateBack() },
            vm = remember { AttestationSettingsViewModel(walletMain.attestationService, walletMain.settingsRepository) },
            onError = { walletMain.errorService.emit(it) }
        )
    }
}

@Composable
private fun LoadCredentialFromUrlContent(
    uri: String,
    flow: SharedDestinationFlow,
    navigator: WalletNavigationController,
    walletMain: WalletMain,
    onClickLogo: () -> Unit,
) {
    var vm by remember { mutableStateOf<LoadCredentialViewModel?>(null) }
    LaunchedEffect(Unit) {
        catchingUnwrapped {
            LoadCredentialViewModel.init(
                walletMain = walletMain,
                navigateUp = navigator::navigateBack,
                url = uri,
                onSubmit = { credentialIdentifierInfo, transactionCode, offer ->
                    walletMain.loadingStatusService.set(LoadingMessageKey.IssuingCredential)
                    navigator.navigate(LoadingRoute(LoadingMessageKey.IssuingCredential))
                    walletMain.scope.launch {
                        try {
                            val issuanceResult = walletMain.provisioningService.loadCredentialWithOffer(
                                credentialOffer = offer!!,
                                credentialIdentifierInfo = credentialIdentifierInfo,
                                transactionCode = transactionCode?.ifEmpty { null }?.ifBlank { null },
                                onProgress = walletMain.loadingStatusService::set,
                            )
                            walletMain.loadingStatusService.clear()
                            if (issuanceResult.credentialIssuanceResult is CredentialIssuanceResult.Success) {
                                navigator.navigateNewGraph(
                                    TransientFlowIssuingResultRoute(issuanceResult.storedEntryIds.firstOrNull())
                                )
                            }
                        } catch (e: Throwable) {
                            walletMain.loadingStatusService.clear()
                            flow.handleCredentialFlowFailure(navigator, walletMain, e)
                        }
                    }
                },
                onClickLogo = onClickLogo,
                onProgress = walletMain.loadingStatusService::set,
            )
        }.onSuccess {
            walletMain.loadingStatusService.clear()
            vm = it
        }.onFailure {
            walletMain.loadingStatusService.clear()
            flow.handleCredentialFlowFailure(navigator, walletMain, it)
        }
    }
    val loadingMessage by walletMain.loadingStatusService.message.collectAsState()
    vm?.let { LoadCredentialView(it) } ?: LoadingView(
        loadingMessageString(loadingMessage ?: LoadingMessageKey.CredentialOffer)
    )
}

private fun SharedDestinationFlow.navigateAfterIntentSuccess(
    navigator: WalletNavigationController,
    route: Route,
) {
    when (this) {
        SharedDestinationFlow.Wallet -> {
            navigator.navigateBack()
            navigator.navigate(route)
        }

        SharedDestinationFlow.Transient -> navigator.navigateNewGraph(route)
    }
}

private fun SharedDestinationFlow.handleCredentialFlowFailure(
    navigator: WalletNavigationController,
    walletMain: WalletMain,
    throwable: Throwable,
) {
    if (this == SharedDestinationFlow.Wallet) {
        navigator.popToInvoker()
    }
    walletMain.errorService.emit(throwable)
}

private fun SharedDestinationFlow.encodeDigitalCredentialOfferReturn(
    value: DigitalCredentialOfferReturn,
): String =
    when (this) {
        SharedDestinationFlow.Wallet -> joseCompliantSerializer.encodeToString(value)
        SharedDestinationFlow.Transient -> joseCompliantSerializer.encodeToString(value)
    }
