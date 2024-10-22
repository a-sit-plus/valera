
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
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
import ui.screens.AuthenticationConsentScreen
import ui.screens.AuthenticationQrCodeScannerView
import ui.screens.AuthenticationSuccessScreen
import ui.screens.CredentialDetailsScreen
import ui.screens.LoadingScreen
import ui.screens.LogScreen
import ui.screens.MyCredentialsScreen
import ui.screens.OnboardingInformationScreen
import ui.screens.OnboardingStartScreen
import ui.screens.OnboardingTermsScreen
import ui.screens.PreAuthQrCodeScannerScreen
import ui.screens.ProvisioningLoadingScreen
import ui.screens.SettingsScreen
import view.AuthenticationQrCodeScannerViewModel

@Composable
fun BottomBar(push: (Route) -> Unit) {
    var current by remember { mutableStateOf(NavigationData.HOME_SCREEN) }
        NavigationBar {
            for (route in listOf(
                NavigationData.HOME_SCREEN,
                NavigationData.AUTHENTICATION_SCANNING_SCREEN,
                NavigationData.INFORMATION_SCREEN,
            )) {
                NavigationBarItem(
                    icon = route.icon,
                    label = {
                        Text(stringResource(route.title))
                    },
                    onClick = {
                        if (current != route) {
                            current = route
                            push(route.destination)
                        }
                    },
                    selected = current == route,
                )
            }
        }
}


@Composable
fun WalletNav(walletMain: WalletMain){
    val navController: NavHostController = rememberNavController()
    // Get current back stack entry
    val backStackEntry by navController.currentBackStackEntryAsState()
    // Get the name of the current screen

    val showScaffold by remember { mutableStateOf(true) }

    val currentScreen = backStackEntry?.destination?.route ?: "HomeScreenRoute"
    val current: Route? = backStackEntry?.toRoute()


    val navigateBack = {
        CoroutineScope(Dispatchers.Main).launch {
            navController.navigateUp()
        }
    }

    val navigate: (Route) -> Unit = { route ->
        CoroutineScope(Dispatchers.Main).launch {
            Napier.d("Navigate to: $route")
            navController.navigate(route)
        }
    }

    IntentHandler(walletMain = walletMain, navigate = navigate)


    Scaffold(
        bottomBar = {
            if (currentScreen.contains("HomeScreenRoute") || currentScreen.contains("SettingsRoute")){
                BottomBar(push = {enum -> navigate(enum)})
            }
        },
        modifier = Modifier,
    ) {
        NavHost(
            navController = navController,
            startDestination = HomeScreenRoute,
            modifier = Modifier
                .fillMaxSize()
        ) {
            composable<HomeScreenRoute> {
                MyCredentialsScreen(
                    navigateToAddCredentialsPage = {
                        navigate(AddCredentialRoute)
                    },
                    navigateToQrAddCredentialsPage = {
                        navigate(PreAuthQrCodeScannerRoute)
                    },
                    navigateToCredentialDetailsPage = {
                        navigate(CredentialDetailsRoute(it))
                    },
                    walletMain = walletMain,
                )
            }
            composable<SettingsRoute> {
                SettingsScreen(navigateToLogPage =  {}, onClickResetApp =  {}, onClickClearLog = {}, walletMain)
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
                AddCredentialScreen(
                    navigateUp = { navigateBack() },
                    walletMain = walletMain,
                    onSubmit = { host, credentialScheme, credentialRepresentation, requestedAttributes ->
                        walletMain.startProvisioning(
                            host = host,
                            credentialScheme = credentialScheme,
                            credentialRepresentation = credentialRepresentation,
                            requestedAttributes = requestedAttributes,
                        ) {
                            navigateBack()
                        }
                    },
                    availableSchemes = walletMain.availableSchemes,
                    hostString = runBlocking { walletMain.walletConfig.host.first() },
                )
            }

            composable<AddCredentialPreAuthnRoute> { backStackEntry ->
                val route: AddCredentialPreAuthnRoute = backStackEntry.toRoute()
                val offer = Json.decodeFromString<CredentialOfferInfo>(route.credentialOfferInfoSerialized)
                AddCredentialScreen(
                    navigateUp = { navController.navigateUp() },
                    walletMain = walletMain,
                    onSubmit = { host, credentialScheme, credentialRepresentation, requestedAttributes ->
                        walletMain.scope.launch {
                            walletMain.provisioningService.loadCredentialWithPreAuthn(
                                credentialIssuer = host,
                                preAuthorizedCode = offer.credentialOffer.grants?.preAuthorizedCode?.preAuthorizedCode.toString(),
                                credentialIdToRequest = offer.credentials
                                    .entries
                                    .first { it.value.first.toScheme() == credentialScheme && it.value.second.toRepresentation() == credentialRepresentation }
                                    .key,

                                )
                            navController.clearBackStack(HomeScreenRoute)
                        }
                    },
                    availableSchemes = offer.credentials.map { it.value.first.toScheme() }.distinct(),
                    hostString = offer.credentialOffer.credentialIssuer,
                    showAttributes = false,
                )
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
                SettingsScreen(
                    navigateToLogPage = {
                        navigate(LogRoute)
                    },
                    onClickResetApp = {
                        val resetMessage = runBlocking {
                            walletMain.resetApp()
                            getString(Res.string.snackbar_reset_app_successfully)
                        }
                        walletMain.snackbarService.showSnackbar(resetMessage)
                        navController.clearBackStack(HomeScreenRoute)
                    },
                    onClickClearLog = {
                        val clearMessage = runBlocking {
                            walletMain.clearLog()
                            getString(Res.string.snackbar_clear_log_successfully)
                        }
                        walletMain.snackbarService.showSnackbar(clearMessage)
                    },
                    walletMain = walletMain,
                )
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

interface Route

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

