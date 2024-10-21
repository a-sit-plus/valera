
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import at.asitplus.wallet.app.common.SnackbarService
import at.asitplus.wallet.app.common.WalletMain
import org.jetbrains.compose.resources.stringResource
import ui.screens.AuthenticationQrCodeScannerView
import ui.screens.MyCredentialsScreen
import ui.screens.OnboardingInformationScreen
import ui.screens.OnboardingStartScreen
import ui.screens.OnboardingTermsScreen
import ui.screens.SettingsScreen
import ui.theme.WalletTheme
import view.AuthenticationQrCodeScannerViewModel

@Composable
fun BottomBar(currentScreen: MainEnums, push: (MainEnums) -> Unit) {
    if (currentScreen == MainEnums.MyCredentialScreen || currentScreen == MainEnums.Settings) {
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
                        if (currentScreen != route.destination) {
                            push(route.destination)
                        }
                    },
                    selected = currentScreen == route.destination,
                )
            }
        }
    }
}

@Composable
fun WalletNav(walletMain: WalletMain){
    val navController: NavHostController = rememberNavController()
    // Get current back stack entry
    val backStackEntry by navController.currentBackStackEntryAsState()
    // Get the name of the current screen
    val currentScreen = MainEnums.valueOf(
        backStackEntry?.destination?.route ?: MainEnums.MyCredentialScreen.name
    )

    Scaffold(
        bottomBar = {
            BottomBar(currentScreen, push = {enum -> navController.navigate(enum.name)})
        },
        modifier = Modifier,
    ) {
        NavHost(
            navController = navController,
            startDestination = MainEnums.MyCredentialScreen.name,
            modifier = Modifier
                .fillMaxSize()
        ) {
            composable(route = MainEnums.MyCredentialScreen.name) {
                MyCredentialsScreen(
                    navigateToAddCredentialsPage = {

                    },
                    navigateToQrAddCredentialsPage = {

                    },
                    navigateToCredentialDetailsPage = {

                    },
                    walletMain = walletMain,
                )
            }
            composable(route = MainEnums.Settings.name) {
                SettingsScreen(navigateToLogPage =  {}, onClickResetApp =  {}, onClickClearLog = {}, walletMain)
            }
            composable(route = MainEnums.QrCodeScanner.name) {
                val vm = AuthenticationQrCodeScannerViewModel(navigateUp = { navController.navigateUp() }, onSuccess = { page ->
                    navController.navigateUp()
                    //navigationStack.push(page)
                }, walletMain = walletMain)
                AuthenticationQrCodeScannerView(vm)
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
    val currentScreen = "onboard?value1=1"



    NavHost(
        navController = navController,
        startDestination = "onboard?value=test",
        modifier = Modifier
            .fillMaxSize()
    ) {
        composable(route = "onboard?value={value}",
            arguments = listOf(navArgument("value") { type = NavType.StringType })) { backStackEntry ->
            backStackEntry.arguments?.getString("value")?.let {
                OnboardingStartScreen(onClickStart = {navController.navigate(OnboardingEnums.Information.name)})
            }
        }
        composable(route = OnboardingEnums.Information.name) {
            OnboardingInformationScreen(onClickContinue = {navController.navigate(OnboardingEnums.Terms.name)})
        }
        composable(route = OnboardingEnums.Terms.name) {
            OnboardingTermsScreen(onClickAccept = { walletMain.walletConfig.set(isConditionsAccepted = true) },
                onClickNavigateBack = {navController.navigateUp()},
                onClickReadDataProtectionPolicy = {},
                onClickReadGeneralTermsAndConditions = {})
        }
    }
}

enum class OnboardingEnums {
    Start,
    Information,
    Terms
}

enum class MainEnums {
    MyCredentialScreen,
    Settings,
    QrCodeScanner
}

@Composable
fun old(walletMain: WalletMain) {
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarService = SnackbarService(walletMain.scope, snackbarHostState)

    try {
        walletMain.initialize(snackbarService)
    } catch (e: Throwable){
        walletMain.errorService.emit(UncorrectableErrorException(e))
    }

    WalletTheme {
        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
            modifier = Modifier.testTag(AppTestTags.rootScaffold)
        ) { _ ->
            Navigator(walletMain)
        }
    }
}