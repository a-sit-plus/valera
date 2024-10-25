package ui.navigation

import PreAuthQrCodeScannerViewModel
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import org.jetbrains.compose.resources.getString
import ui.composables.BottomBar
import ui.composables.NavigationData
import ui.navigation.Routes.AddCredentialPreAuthnRoute
import ui.navigation.Routes.AddCredentialRoute
import ui.navigation.Routes.AuthenticationConsentRoute
import ui.navigation.Routes.AuthenticationLoadingRoute
import ui.navigation.Routes.AuthenticationQrCodeScannerRoute
import ui.navigation.Routes.AuthenticationSuccessRoute
import ui.navigation.Routes.CredentialDetailsRoute
import ui.navigation.Routes.HomeScreenRoute
import ui.navigation.Routes.LoadingRoute
import ui.navigation.Routes.LogRoute
import ui.navigation.Routes.PreAuthQrCodeScannerRoute
import ui.navigation.Routes.ProvisioningLoadingRoute
import ui.navigation.Routes.Route
import ui.navigation.Routes.SettingsRoute
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
import ui.screens.PreAuthQrCodeScannerScreen
import ui.screens.ProvisioningLoadingScreen
import ui.screens.SettingsView
import ui.screens.SettingsViewModel
import view.AuthenticationQrCodeScannerViewModel

@Composable
fun WalletNavigation(walletMain: WalletMain){
    val navController: NavHostController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()

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
                bottomBar = { BottomBar(navigate = navigate, selected = NavigationData.HOME_SCREEN) }
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
                navigateUp = {navigateBack()},
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
            SettingsView(vm = vm, bottomBar = { BottomBar(navigate = navigate, selected = NavigationData.INFORMATION_SCREEN) })
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