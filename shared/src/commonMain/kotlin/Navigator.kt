
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import at.asitplus.wallet.app.common.CredentialOfferInfo
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.oidc.AuthenticationRequestParametersFrom
import at.asitplus.wallet.lib.oidc.helpers.AuthorizationResponsePreparationState
import at.asitplus.wallet.lib.oidvci.toRepresentation
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.navigation_button_label_my_data
import compose_wallet_app.shared.generated.resources.navigation_button_label_settings
import compose_wallet_app.shared.generated.resources.navigation_button_label_show_data
import compose_wallet_app.shared.generated.resources.snackbar_clear_log_successfully
import compose_wallet_app.shared.generated.resources.snackbar_reset_app_successfully
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import ui.composables.BottomBar
import ui.navigation.AddCredentialPreAuthnRoute
import ui.navigation.AddCredentialRoute
import ui.navigation.AuthenticationConsentRoute
import ui.navigation.AuthenticationLoadingRoute
import ui.navigation.AuthenticationQrCodeScannerRoute
import ui.navigation.AuthenticationSuccessRoute
import ui.navigation.CredentialDetailsRoute
import ui.navigation.HomeScreenRoute
import ui.navigation.IntentHandler
import ui.navigation.LoadingRoute
import ui.navigation.LogRoute
import ui.navigation.OnboardingInformation
import ui.navigation.OnboardingStart
import ui.navigation.OnboardingTerms
import ui.navigation.PreAuthQrCodeScannerRoute
import ui.navigation.ProvisioningLoadingRoute
import ui.navigation.SettingsRoute
import ui.screens.AddCredentialScreen
import ui.screens.AddCredentialViewModel
import ui.screens.AuthenticationConsentScreen
import ui.screens.AuthenticationQrCodeScannerView
import ui.screens.AuthenticationSuccessScreen
import ui.screens.CredentialDetailsScreen
import ui.screens.CredentialsView
import ui.screens.CredentialsViewModel
import ui.screens.LoadingScreen
import ui.screens.LogScreen
import ui.screens.OnboardingInformationScreen
import ui.screens.OnboardingStartScreen
import ui.screens.OnboardingTermsScreen
import ui.screens.PreAuthQrCodeScannerScreen
import ui.screens.ProvisioningLoadingScreen
import ui.screens.SettingsView
import ui.screens.SettingsViewModel
import view.AuthenticationQrCodeScannerViewModel

@Composable
fun WalletNav(walletMain: WalletMain){
    val navController: NavHostController = rememberNavController()
    // Get current back stack entry
    val backStackEntry by navController.currentBackStackEntryAsState()
    // Get the name of the current screen

    val showScaffold by remember { mutableStateOf(true) }

    val currentScreen = backStackEntry?.destination?.route ?: "HomeScreenRoute"
    val current: Route? = backStackEntry?.toRoute()


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

    IntentHandler(walletMain = walletMain, navigate = navigate, navigateBack = navigateBack)

    NavHost(
        navController = navController,
        startDestination = HomeScreenRoute,
        modifier = Modifier
            .fillMaxSize()
    ) {
        composable<HomeScreenRoute> {
            val vm = CredentialsViewModel(walletMain)
            CredentialsView(
                navigateToAddCredentialsPage = {
                    navigate(AddCredentialRoute)
                },
                navigateToQrAddCredentialsPage = {
                    navigate(PreAuthQrCodeScannerRoute)
                },
                navigateToCredentialDetailsPage = {
                    navigate(CredentialDetailsRoute(it))
                },
                vm = vm,
                imageDecoder = {
                    try {
                        walletMain.platformAdapter.decodeImage(it)
                    } catch (throwable: Throwable) {
                        // TODO: should this be emitted to the error service?
                        Napier.w("Failed Operation: decodeImage")
                        null
                    }
                },
                bottomBar = {BottomBar(navigate = navigate, selected = NavigationData.HOME_SCREEN)}
            )
        }
        composable<AuthenticationQrCodeScannerRoute> {
            val vm = AuthenticationQrCodeScannerViewModel(navigateUp = { navigateBack() }, onSuccess = { route ->
                val test = backStackEntry
                Napier.d(test.toString())
                navigate(route)
            }, walletMain = walletMain)
            AuthenticationQrCodeScannerView(vm)
        }
        composable<AuthenticationConsentRoute> { backStackEntry ->
            val route: AuthenticationConsentRoute = backStackEntry.toRoute()

            val request =
                AuthenticationRequestParametersFrom.deserialize(route.authenticationRequestParametersFromSerialized)
                    .getOrThrow()
            val preparationState =
                vckJsonSerializer.decodeFromString<AuthorizationResponsePreparationState>(
                    route.authorizationPreparationStateSerialized,
                )

            AuthenticationConsentScreen(
                spName = null,
                spLocation = route.recipientLocation,
                spImage = null,
                authenticationRequestParametersFrom = request,
                authorizationResponsePreparationState = preparationState,
                navigateUp = {navController.navigateUp()},
                navigateToAuthenticationSuccessPage = {
                    navigate(AuthenticationSuccessRoute)
                },
                walletMain = walletMain,
            )
        }

        composable<AuthenticationSuccessRoute> { backStackEntry ->
            AuthenticationSuccessScreen(
                navigateUp = { navigateBack() },
            )
        }

        composable<AddCredentialRoute> { backStackEntry ->
            val vm = AddCredentialViewModel(walletMain = walletMain,
                navigateUp = navigateBack,
                hostString = runBlocking { walletMain.walletConfig.host.first() }, availableSchemes = walletMain.availableSchemes, onSubmit = { host, credentialScheme, credentialRepresentation, requestedAttributes ->
                    popBackStack(HomeScreenRoute)
                    walletMain.scope.launch {
                        walletMain.startProvisioning(
                            host = host,
                            credentialScheme = credentialScheme,
                            credentialRepresentation = credentialRepresentation,
                            requestedAttributes = requestedAttributes,
                        ) {
                        }
                    }

                })
            AddCredentialScreen(vm)
        }

        composable<AddCredentialPreAuthnRoute> { backStackEntry ->
            val route: AddCredentialPreAuthnRoute = backStackEntry.toRoute()
            val offer = Json.decodeFromString<CredentialOfferInfo>(route.credentialOfferInfoSerialized)
            val vm = AddCredentialViewModel(walletMain = walletMain,
                navigateUp = navigateBack,
                hostString = offer.credentialOffer.credentialIssuer,
                availableSchemes = offer.credentials.map { it.value.first.toScheme() }.distinct(),
                onSubmit = { host, credentialScheme, credentialRepresentation, requestedAttributes ->
                    popBackStack(HomeScreenRoute)
                    navigate(LoadingRoute)
                    walletMain.scope.launch {
                        try {
                            walletMain.provisioningService.loadCredentialWithPreAuthn(
                                credentialIssuer = host,
                                preAuthorizedCode = offer.credentialOffer.grants?.preAuthorizedCode?.preAuthorizedCode.toString(),
                                credentialIdToRequest = offer.credentials
                                    .entries
                                    .first { it.value.first.toScheme() == credentialScheme && it.value.second.toRepresentation() == credentialRepresentation }
                                    .key
                            )
                        } catch (e: Throwable){
                            popBackStack(HomeScreenRoute)
                            walletMain.errorService.emit(e)
                        }
                        popBackStack(HomeScreenRoute)
                    }
                }
            )
            AddCredentialScreen(vm)
        }

        composable<CredentialDetailsRoute> { backStackEntry ->
            val route: CredentialDetailsRoute = backStackEntry.toRoute()

            CredentialDetailsScreen(
                storeEntryId = route.storeEntryId,
                navigateUp = { navigateBack() },
                walletMain = walletMain,
            )
        }

        composable<ProvisioningLoadingRoute> { backStackEntry ->
            val route: ProvisioningLoadingRoute = backStackEntry.toRoute()

            ProvisioningLoadingScreen(
                link = route.link,
                navigateUp = { navigateBack() },
                walletMain = walletMain,
            )
        }

        composable<SettingsRoute> { backStackEntry ->
            val vm = SettingsViewModel(
                onClickShareLogFile = {
                    navigate(LogRoute)
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
                walletMain = walletMain,
            )
            SettingsView(vm = vm, bottomBar = {BottomBar(navigate = navigate, selected = NavigationData.INFORMATION_SCREEN)})
        }

        composable<PreAuthQrCodeScannerRoute> { backStackEntry ->
            val vm = PreAuthQrCodeScannerViewModel(
                walletMain = walletMain,
                navigateUp = { navigateBack() },
                navigateToAddCredentialsPage = { offer ->
                    navigate(AddCredentialPreAuthnRoute(Json.encodeToString(offer)))
                })
            PreAuthQrCodeScannerScreen(vm)
        }

        composable<LogRoute> { backStackEntry ->
            LogScreen(
                navigateUp = { navigateBack() },
                walletMain = walletMain,
            )
        }

        composable<LoadingRoute> { backStackEntry ->
            LoadingScreen()
        }

        composable<AuthenticationQrCodeScannerRoute> { backStackEntry ->
            val vm = AuthenticationQrCodeScannerViewModel(navigateUp = { navigateBack() }, onSuccess = { route ->
                navigateBack()
                navigate(route)
            }, walletMain = walletMain)
            AuthenticationQrCodeScannerView(vm)
        }

        composable<AuthenticationLoadingRoute> { backStackEntry ->
            LoadingScreen()
        }

    }
}

@Composable
fun OnboardingNav(walletMain: WalletMain) {
    val navController: NavHostController = rememberNavController()
    // Get current back stack entry
    val backStackEntry by navController.currentBackStackEntryAsState()
    // Get the name of the current screen
    val currentScreen = backStackEntry?.destination?.route



    NavHost(
        navController = navController,
        startDestination = OnboardingStart,
        modifier = Modifier
            .fillMaxSize()
    ) {
        composable<OnboardingStart> {
            OnboardingStartScreen(onClickStart = {navController.navigate(OnboardingInformation)})
        }
        composable<OnboardingInformation> {
            OnboardingInformationScreen(onClickContinue = {navController.navigate(OnboardingTerms)})
        }
        composable<OnboardingTerms> {
            OnboardingTermsScreen(onClickAccept = { walletMain.walletConfig.set(isConditionsAccepted = true) },
                onClickNavigateBack = {navController.navigateUp()},
                onClickReadDataProtectionPolicy = {},
                onClickReadGeneralTermsAndConditions = {})
        }
    }
}

@Serializable
open class Route()

enum class NavigationData(
    val title: StringResource,
    val icon: @Composable () -> Unit,
    val destination: Route,
    val isActive: (Route) -> Boolean
) {
    HOME_SCREEN(
        title = Res.string.navigation_button_label_my_data,
        icon = {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
            )
        },
        destination = HomeScreenRoute,
        isActive = {
            when (it) {
                is HomeScreenRoute -> true
                else -> false
            }
        },
    ),
    AUTHENTICATION_SCANNING_SCREEN(
        title = Res.string.navigation_button_label_show_data,
        icon = {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = null,
            )
        },
        destination = AuthenticationQrCodeScannerRoute,
        isActive = {
            when (it) {
                is AuthenticationQrCodeScannerRoute -> true
                else -> false
            }
        },
    ),
    INFORMATION_SCREEN(
        title = Res.string.navigation_button_label_settings,
        icon = {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
            )
        },
        destination = SettingsRoute,
        isActive = {
            when (it) {
                is SettingsRoute -> true
                else -> false
            }
        },
    ),
}

