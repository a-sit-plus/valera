package ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import at.asitplus.wallet.app.common.WalletMain
import ui.navigation.Routes.OnboardingInformation
import ui.navigation.Routes.OnboardingStart
import ui.navigation.Routes.OnboardingTerms
import ui.screens.OnboardingInformationScreen
import ui.screens.OnboardingStartScreen
import ui.screens.OnboardingTermsScreen

@Composable
fun OnboardingNavigation(walletMain: WalletMain) {
    val navController: NavHostController = rememberNavController()

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