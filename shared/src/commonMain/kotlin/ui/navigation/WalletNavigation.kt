package ui.navigation

import AppTestTags
import Globals
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import at.asitplus.catchingUnwrapped
import at.asitplus.openid.AuthenticationRequestParameters
import at.asitplus.openid.CredentialOffer
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.rqes.SignatureRequestParameters
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.error_feature_not_yet_available
import at.asitplus.valera.resources.snackbar_clear_log_successfully
import at.asitplus.valera.resources.snackbar_reset_app_successfully
import at.asitplus.wallet.app.common.ErrorService
import at.asitplus.wallet.app.common.SnackbarService
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.dcapi.DCAPIRequest
import at.asitplus.wallet.app.common.decodeImage
import at.asitplus.wallet.app.common.domain.platform.UrlOpener
import at.asitplus.wallet.lib.data.dif.ConstraintFieldsEvaluationException
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.openid.AuthorizationResponsePreparationState
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.getString
import org.koin.compose.koinInject
import ui.composables.BottomBar
import ui.composables.NavigationData
import ui.navigation.routes.AddCredentialPreAuthnRoute
import ui.navigation.routes.AddCredentialRoute
import ui.navigation.routes.AuthenticationSuccessRoute
import ui.navigation.routes.AuthenticationViewRoute
import ui.navigation.routes.AuthorizationIntentRoute
import ui.navigation.routes.CredentialDetailsRoute
import ui.navigation.routes.DCAPIAuthenticationConsentRoute
import ui.navigation.routes.DCAPIAuthorizationIntentRoute
import ui.navigation.routes.ErrorIntentRoute
import ui.navigation.routes.ErrorRoute
import ui.navigation.routes.HomeScreenRoute
import ui.navigation.routes.LoadCredentialRoute
import ui.navigation.routes.LoadingRoute
import ui.navigation.routes.LocalPresentationAuthenticationConsentRoute
import ui.navigation.routes.LogRoute
import ui.navigation.routes.OnboardingInformationRoute
import ui.navigation.routes.OnboardingStartRoute
import ui.navigation.routes.OnboardingWrapperTestTags
import ui.navigation.routes.PresentDataRoute
import ui.navigation.routes.PresentationIntentRoute
import ui.navigation.routes.ProvisioningIntentRoute
import ui.navigation.routes.QrCodeScannerRoute
import ui.navigation.routes.Route
import ui.navigation.routes.SettingsRoute
import ui.navigation.routes.ShowQrCodeRoute
import ui.navigation.routes.SigningCredentialIntentRoute
import ui.navigation.routes.SigningIntentRoute
import ui.navigation.routes.SigningPreloadIntentRoute
import ui.navigation.routes.SigningQtspSelectionRoute
import ui.navigation.routes.SigningServiceIntentRoute
import ui.navigation.routes.VerifyDataRoute
import ui.viewmodels.AddCredentialViewModel
import ui.viewmodels.CredentialDetailsViewModel
import ui.viewmodels.CredentialsViewModel
import ui.viewmodels.ErrorViewModel
import ui.viewmodels.LoadCredentialViewModel
import ui.viewmodels.LogViewModel
import ui.viewmodels.QrCodeScannerMode
import ui.viewmodels.QrCodeScannerViewModel
import ui.viewmodels.SettingsViewModel
import ui.viewmodels.SigningQtspSelectionViewModel
import ui.viewmodels.authentication.AuthenticationSuccessViewModel
import ui.viewmodels.authentication.DCAPIAuthenticationViewModel
import ui.viewmodels.authentication.DefaultAuthenticationViewModel
import ui.viewmodels.authentication.PresentationViewModel
import ui.viewmodels.intents.AuthorizationIntentViewModel
import ui.viewmodels.intents.DCAPIAuthorizationIntentViewModel
import ui.viewmodels.intents.ErrorIntentViewModel
import ui.viewmodels.intents.PresentationIntentViewModel
import ui.viewmodels.intents.ProvisioningIntentViewModel
import ui.viewmodels.intents.SigningCredentialIntentViewModel
import ui.viewmodels.intents.SigningIntentViewModel
import ui.viewmodels.intents.SigningPreloadIntentViewModel
import ui.viewmodels.intents.SigningServiceIntentViewModel
import ui.viewmodels.iso.ShowQrCodeViewModel
import ui.viewmodels.iso.VerifierViewModel
import ui.views.CredentialDetailsView
import ui.views.CredentialsView
import ui.views.ErrorView
import ui.views.LoadCredentialView
import ui.views.LoadingView
import ui.views.LogView
import ui.views.OnboardingInformationView
import ui.views.OnboardingStartView
import ui.views.PresentDataView
import ui.views.QrCodeScannerView
import ui.views.SelectIssuingServerView
import ui.views.SettingsView
import ui.views.SigningQtspSelectionView
import ui.views.authentication.AuthenticationSuccessView
import ui.views.authentication.AuthenticationView
import ui.views.intents.AuthorizationIntentView
import ui.views.intents.DCAPIAuthorizationIntentView
import ui.views.intents.ErrorIntentView
import ui.views.intents.PresentationIntentView
import ui.views.intents.ProvisioningIntentView
import ui.views.intents.SigningCredentialIntentView
import ui.views.intents.SigningIntentView
import ui.views.intents.SigningPreloadIntentView
import ui.views.intents.SigningServiceIntentView
import ui.views.iso.ShowQrCodeView
import ui.views.iso.verifier.VerifierView
import ui.views.presentation.PresentationView

internal object NavigatorTestTags {
    const val loadingTestTag = "loadingTestTag"
}

@Composable
fun WalletNavigation(
    settingsViewModel: SettingsViewModel = koinInject(),
    intentService: IntentService = koinInject(),
    snackbarService: SnackbarService = koinInject(),
    errorService: ErrorService = koinInject(),
    urlOpener: UrlOpener = koinInject(),
) {
    val navController: NavHostController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

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

    val onClickLogo = {
        urlOpener("https://wallet.a-sit.at/")
    }

    val isConditionsAccepted = settingsViewModel.isConditionsAccepted.collectAsState(null)

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
            navigate,
            navigateBack,
            popBackStack,
            onClickLogo,
            onError = { e ->
                popBackStack(HomeScreenRoute)
                errorService.emit(e)
            },
        )
    }

    LaunchedEffect(null) {
        this.launch {
            Globals.appLink.combineTransform(intentService.readyForIntents) { link, ready ->
                if (ready == true && link != null) {
                    emit(link)
                }
            }.collect { link ->
                Napier.d("appLink.combineTransform $link")
                catchingUnwrapped {
                    val route = intentService.handleIntent(link)
                    navigate(route)
                }.onFailure {
                    errorService.emit(it)
                }
                Globals.appLink.value = null
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
            errorService.error.collect { (throwable) ->
                navigate(
                    ErrorRoute(
                        throwable.enrichMessage(),
                        throwable.cause?.message ?: throwable.cause?.toString()
                    )
                )
            }
        }
    }
}

private fun Throwable.enrichMessage() = when (this) {
    is ConstraintFieldsEvaluationException -> "$message ${constraintFieldExceptions.keys}"
    else -> message ?: toString()
}

@Composable
private fun WalletNavHost(
    navController: NavHostController,
    startDestination: Route,
    navigate: (Route) -> Unit,
    navigateBack: () -> Unit,
    popBackStack: (Route) -> Unit,
    onClickLogo: () -> Unit,
    onError: (Throwable) -> Unit,
    walletMain: WalletMain = koinInject(),
    settingsViewModel: SettingsViewModel = koinInject(),
    intentService: IntentService = koinInject(),
) {
    val currentHost by settingsViewModel.host.collectAsState("")
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
                onClickLogo = onClickLogo,
                modifier = Modifier.testTag(OnboardingWrapperTestTags.onboardingStartScreen)
            )
        }
        composable<OnboardingInformationRoute> {
            OnboardingInformationView(
                onClickContinue = { settingsViewModel.set(isConditionsAccepted = true) },
                onClickLogo = onClickLogo
            )
        }
        composable<HomeScreenRoute> {
            CredentialsView(
                vm = remember {
                    CredentialsViewModel(
                        walletMain,
                        navigateToAddCredentialsPage = {
                            navigate(AddCredentialRoute)
                        },
                        navigateToQrAddCredentialsPage = {
                            navigate(QrCodeScannerRoute(QrCodeScannerMode.PROVISIONING))
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
                        },
                        onClickLogo = onClickLogo,
                        onClickSettings = { navigate(SettingsRoute) }
                    )
                },
                bottomBar = {
                    BottomBar(
                        navigate = { route -> navigate(route) },
                        selected = NavigationData.HOME_SCREEN
                    )
                }
            )
            LaunchedEffect(null) {
                walletMain.scope.launch {
                    intentService.readyForIntents.emit(true)
                }
            }
        }

        composable<PresentDataRoute> {
            PresentDataView(
                onNavigateToAuthenticationQrCodeScannerView = {
                    navigate(QrCodeScannerRoute(QrCodeScannerMode.AUTHENTICATION))
                },
                onNavigateToShowQrCodeView = { navigate(ShowQrCodeRoute) },
                onClickLogo = onClickLogo,
                onClickSettings = { navigate(SettingsRoute) },
                bottomBar = {
                    BottomBar(
                        navigate = navigate,
                        selected = NavigationData.PRESENT_DATA_SCREEN
                    )
                }
            )
        }

        composable<ShowQrCodeRoute> {
            ShowQrCodeView(remember {
                ShowQrCodeViewModel(
                    walletMain = walletMain,
                    navigateUp = { navigateBack() },
                    onClickLogo = onClickLogo,
                    onClickSettings = { navigate(SettingsRoute) },
                    onNavigateToPresentmentScreen = {
                        Globals.presentationStateModel.value = it
                        navigate(LocalPresentationAuthenticationConsentRoute("QR"))
                    },
                    settingsViewModel = settingsViewModel,
                )
            })
        }

        composable<VerifyDataRoute> {
            VerifierView(
                vm = remember {
                    VerifierViewModel(
                        navigateUp = { navigateBack() },
                        onClickLogo = onClickLogo,
                        walletMain = walletMain,
                        navigateToHomeScreen = { popBackStack(HomeScreenRoute) },
                        onClickSettings = { navigate(SettingsRoute) },
                        settingsViewModel = settingsViewModel,
                    )
                },
                onError = onError,
                bottomBar = {
                    BottomBar(
                        navigate = navigate,
                        selected = NavigationData.VERIFY_DATA_SCREEN
                    )
                }
            )
        }

        composable<AuthenticationViewRoute> { backStackEntry ->
            val route: AuthenticationViewRoute = backStackEntry.toRoute()

            val vm = remember {
                try {
                    val request: RequestParametersFrom<AuthenticationRequestParameters> =
                        vckJsonSerializer.decodeFromString(route.authenticationRequestParametersFromSerialized)
                    val preparationState: AuthorizationResponsePreparationState =
                        vckJsonSerializer.decodeFromString(route.authorizationPreparationStateSerialized)

                    DefaultAuthenticationViewModel(
                        spName = null,
                        spLocation = route.recipientLocation,
                        spImage = null,
                        authenticationRequest = request,
                        preparationState = preparationState,
                        navigateUp = navigateBack,
                        navigateToAuthenticationSuccessPage = {
                            navigate(AuthenticationSuccessRoute(it, route.isCrossDeviceFlow))
                        },
                        navigateToHomeScreen = {
                            popBackStack(HomeScreenRoute)
                        },
                        walletMain = walletMain,
                        onClickLogo = onClickLogo,
                        onClickSettings = { navigate(SettingsRoute) }
                    )
                } catch (e: Throwable) {
                    popBackStack(HomeScreenRoute)
                    walletMain.errorService.emit(e)
                    null
                }
            }

            if (vm != null) {
                AuthenticationView(
                    vm = vm,
                    onError = onError
                )
            }
        }

        composable<DCAPIAuthenticationConsentRoute> { backStackEntry ->
            val vm = remember {
                try {
                    val dcApiRequest =
                        DCAPIRequest.deserialize(backStackEntry.toRoute<DCAPIAuthenticationConsentRoute>().apiRequestSerialized)
                            .getOrThrow()

                    DCAPIAuthenticationViewModel(
                        dcApiRequest = dcApiRequest,
                        navigateUp = navigateBack,
                        navigateToAuthenticationSuccessPage = {
                            navigate(AuthenticationSuccessRoute(it, false))
                        },
                        walletMain = walletMain,
                        navigateToHomeScreen = {
                            popBackStack(HomeScreenRoute)
                        },
                        onClickLogo = onClickLogo,
                        onClickSettings = { navigate(SettingsRoute) }
                    )
                } catch (e: Throwable) {
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
                    Globals.presentationStateModel.value?.let {
                        PresentationViewModel(
                            presentationStateModel = it,
                            navigateUp = { popBackStack(HomeScreenRoute) },
                            onAuthenticationSuccess = { },
                            navigateToHomeScreen = { popBackStack(HomeScreenRoute) },
                            walletMain = walletMain,
                            onClickLogo = onClickLogo,
                            onClickSettings = { navigate(SettingsRoute) }
                        )
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
                    }
                )
            }
        }

        composable<AuthenticationSuccessRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<AuthenticationSuccessRoute>()
            AuthenticationSuccessView(vm = remember {
                AuthenticationSuccessViewModel(
                    navigateUp = navigateBack,
                    onClickLogo = onClickLogo,
                    isCrossDeviceFlow = route.isCrossDeviceFlow,
                    openRedirectUrl = route.redirectUrl?.let {
                        { walletMain.platformAdapter.openUrl(it) }
                    },
                    onClickSettings = { navigate(SettingsRoute) }
                )
            })
        }

        composable<AddCredentialRoute> { backStackEntry ->
            SelectIssuingServerView(remember {
                AddCredentialViewModel(
                    walletMain = walletMain,
                    navigateUp = navigateBack,
                    hostString = currentHost,
                    onSubmitServer = { host ->
                        navigate(LoadCredentialRoute(host))
                    },
                    onClickLogo = onClickLogo,
                    onClickSettings = { navigate(SettingsRoute) }
                )
            })
        }

        composable<LoadCredentialRoute> { backStackEntry ->
            remember {
                runBlocking {
                    runCatching {
                        LoadCredentialViewModel.init(
                            walletMain = walletMain,
                            navigateUp = navigateBack,
                            hostString = backStackEntry.toRoute<LoadCredentialRoute>().host,
                            onSubmit = { credentialIdentifierInfo, _ ->
                                popBackStack(HomeScreenRoute)
                                walletMain.scope.launch {
                                    walletMain.startProvisioning(
                                        host = backStackEntry.toRoute<LoadCredentialRoute>().host,
                                        credentialIdentifierInfo = credentialIdentifierInfo,
                                    ) {
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
            val offer =
                Json.decodeFromString<CredentialOffer>(backStackEntry.toRoute<AddCredentialPreAuthnRoute>().credentialOfferSerialized)
            remember {
                runBlocking {
                    runCatching {
                        LoadCredentialViewModel.init(
                            walletMain = walletMain,
                            navigateUp = navigateBack,
                            offer = offer,
                            onSubmit = { credentialIdentifierInfo, transactionCode ->
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

        composable<CredentialDetailsRoute> { backStackEntry ->
            CredentialDetailsView(vm = remember {
                CredentialDetailsViewModel(
                    storeEntryId = backStackEntry.toRoute<CredentialDetailsRoute>().storeEntryId,
                    navigateUp = navigateBack,
                    walletMain = walletMain,
                    onClickLogo = onClickLogo,
                    onClickSettings = { navigate(SettingsRoute) }
                )
            })
        }

        composable<SettingsRoute> { backStackEntry ->
            SettingsView(
                buildType = walletMain.buildContext.buildType,
                version = walletMain.buildContext.versionName,
                onClickShareLogFile = {
                    navigate(LogRoute)
                },
                onClickResetApp = {
                    walletMain.scope.launch {
                        walletMain.resetApp()
                        val resetMessage = getString(Res.string.snackbar_reset_app_successfully)
                        walletMain.snackbarService.showSnackbar(resetMessage)
                        navController.popBackStack(route = HomeScreenRoute, inclusive = false)
                    }
                },
                onClickClearLogFile = {
                    walletMain.scope.launch {
                        walletMain.clearLog()
                        val clearMessage = getString(Res.string.snackbar_clear_log_successfully)
                        walletMain.snackbarService.showSnackbar(clearMessage)
                    }
                },
                onClickLogo = onClickLogo,
                onClickSettings = { popBackStack(HomeScreenRoute) },
                onClickBack = navigateBack,
                onClickFAQs = {
                    walletMain.scope.launch {
                        walletMain.snackbarService.showSnackbar(getString(Res.string.error_feature_not_yet_available))
                    }
                },
                onClickDataProtectionPolicy = {
                    walletMain.scope.launch {
                        walletMain.snackbarService.showSnackbar(getString(Res.string.error_feature_not_yet_available))
                    }
                },
                onClickLicenses = {
                    walletMain.scope.launch {
                        walletMain.snackbarService.showSnackbar(getString(Res.string.error_feature_not_yet_available))
                    }
                },
            )
        }

        composable<LogRoute> { backStackEntry ->
            LogView(vm = remember {
                LogViewModel(
                    navigateUp = navigateBack,
                    walletMain = walletMain,
                    onClickLogo = onClickLogo,
                    onClickSettings = { navigate(SettingsRoute) }
                )
            })
        }

        composable<ErrorRoute> { backStackEntry ->
            ErrorView(remember {
                ErrorViewModel(
                    resetStack = { popBackStack(HomeScreenRoute) },
                    message = backStackEntry.toRoute<ErrorRoute>().message,
                    cause = backStackEntry.toRoute<ErrorRoute>().cause,
                    onClickLogo = onClickLogo,
                    onClickSettings = { navigate(SettingsRoute) }
                )
            })
        }

        composable<LoadingRoute> { backStackEntry ->
            LoadingView()
        }

        composable<SigningQtspSelectionRoute> { backStackEntry ->
            SigningQtspSelectionView(vm = remember {
                SigningQtspSelectionViewModel(
                    navigateUp = navigateBack,
                    onContinue = { signatureRequestParametersSerialized ->
                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                val signatureRequestParameters =
                                    vckJsonSerializer.decodeFromString<SignatureRequestParameters>(
                                        signatureRequestParametersSerialized
                                    )
                                walletMain.signingService.start(signatureRequestParameters)

                            } catch (e: Throwable) {
                                walletMain.errorService.emit(e)
                            }
                        }
                    },
                    walletMain = walletMain,
                    onClickLogo = onClickLogo,
                    onClickSettings = { navigate(SettingsRoute) },
                    signatureRequestParametersSerialized = backStackEntry.toRoute<SigningQtspSelectionRoute>().signatureRequestParametersSerialized
                )
            })
        }

        composable<ProvisioningIntentRoute> { backStackEntry ->
            ProvisioningIntentView(remember {
                ProvisioningIntentViewModel(
                    walletMain = walletMain,
                    uri = backStackEntry.toRoute<ProvisioningIntentRoute>().uri,
                    onSuccess = {
                        navigateBack()
                    }, onFailure = { error ->
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
                    onFailure = {
                        walletMain.errorService.emit(Exception("Invalid Authentication Request"))
                    })
            })

        }

        composable<PresentationIntentRoute> { backStackEntry ->
            PresentationIntentView(remember {
                PresentationIntentViewModel(
                    walletMain = walletMain,
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
            }
            )
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
                            val signatureRequestParameters =
                                walletMain.signingService.parseSignatureRequestParameter(backStackEntry.toRoute<SigningIntentRoute>().uri)
                            navigate(
                                SigningQtspSelectionRoute(
                                    vckJsonSerializer.encodeToString(
                                        signatureRequestParameters
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
                }
            )
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
    }
}
