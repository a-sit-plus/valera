
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import at.asitplus.wallet.app.common.Configuration
import at.asitplus.wallet.app.common.SnackbarService
import at.asitplus.wallet.app.common.WalletMain
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.navigation_button_label_my_data
import compose_wallet_app.shared.generated.resources.navigation_button_label_settings
import compose_wallet_app.shared.generated.resources.navigation_button_label_show_data
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.stringResource
import ui.composables.CategorySelectionRowDefaults.Companion.modifier
import ui.navigation.AddCredentialPage
import ui.navigation.CredentialDetailsPage
import ui.navigation.HomePage
import ui.navigation.PreAuthQrCodeScannerPage
import ui.navigation.SettingsPage
import ui.screens.AuthenticationQrCodeScannerView
import ui.screens.MyCredentialsScreen
import ui.screens.OnboardingInformationScreen
import ui.screens.OnboardingNavigator
import ui.screens.OnboardingStartScreen
import ui.screens.OnboardingStartScreenTestTag
import ui.screens.OnboardingTermsScreen
import ui.screens.OnboardingWrapperTestTags
import ui.screens.PreAuthQrCodeScannerScreen
import ui.screens.SettingsScreen
import ui.theme.WalletTheme
import view.AuthenticationQrCodeScannerViewModel

/**
 * Global variable which especially helps to channel information from swift code
 * to compose whenever the app gets called via an associated domain
 */
var appLink = mutableStateOf<String?>(null)

/**
 * Global variable to test at least something from the iOS UITest
 */
var iosTestValue = Configuration.IOS_TEST_VALUE

internal object AppTestTags {
    const val rootScaffold = "rootScaffold"
}



@Composable
fun App(walletMain: WalletMain) {
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarService = SnackbarService(walletMain.scope, snackbarHostState)

    try {
        walletMain.initialize(snackbarService)
    } catch (e: Throwable){
        walletMain.errorService.emit(UncorrectableErrorException(e))
    }


    val isConditionsAccepted by walletMain.walletConfig.isConditionsAccepted.collectAsState(null)

    when (isConditionsAccepted) {
        null -> {
            Box(modifier = Modifier.testTag(OnboardingWrapperTestTags.onboardingLoadingIndicator))
        }
        true -> {
            WalletTheme {
                Scaffold(
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState)
                    },
                    modifier = Modifier.testTag(AppTestTags.rootScaffold)
                ) { _ ->
                    WalletNav(walletMain)
                }
            }
        }
        false -> OnboardingNav(walletMain)
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
            if (currentScreen == MainEnums.MyCredentialScreen || currentScreen == MainEnums.Settings) {
                NavigationBar {
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                            )
                        },
                        label = {
                            Text(stringResource(Res.string.navigation_button_label_my_data))
                        },
                        onClick = {
                            navController.navigate(MainEnums.MyCredentialScreen.name)
                        },
                        selected = false,
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                            )
                        },
                        label = {
                            Text(stringResource(Res.string.navigation_button_label_show_data))
                        },
                        onClick = {
                            navController.navigate(MainEnums.QrCodeScanner.name)
                        },
                        selected = false,
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                            )
                        },
                        label = {
                            Text(stringResource(Res.string.navigation_button_label_settings))
                        },
                        onClick = {
                            navController.navigate(MainEnums.Settings.name)
                        },
                        selected = false,
                    )
                }
            }
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

expect fun getPlatformName(): String

@Composable
expect fun getColorScheme(): ColorScheme