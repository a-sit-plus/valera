import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import at.asitplus.wallet.app.common.WalletMain
import navigation.NavigationStack
import navigation.OnboardingInformationPage
import navigation.OnboardingPage
import navigation.OnboardingStartPage
import navigation.OnboardingTermsPage
import view.OnboardingInformationScreen
import view.OnboardingStartScreen
import view.OnboardingTermsScreen

@Composable
fun OnboardingWrapper(
    walletMain: WalletMain,
    content: @Composable () -> Unit,
) {
    val isConditionsAccepted by walletMain.walletConfig.isConditionsAccepted.collectAsState(null)

    if (isConditionsAccepted != true) {
        OnboardingScreenNavigator(
            onOnboardingComplete = {
                walletMain.walletConfig.set(isConditionsAccepted = true)
            },
        )
    } else {
        content()
    }
}

@Composable
fun OnboardingScreenNavigator(
    onOnboardingComplete: () -> Unit,
) {
    // Modified from https://github.com/JetBrains/compose-multiplatform/tree/master/examples/imageviewer
    val navigationStack = rememberSaveable(
        saver = listSaver(
            restore = { NavigationStack(*it.toTypedArray()) },
            save = { it.stack },
        )
    ) {
        NavigationStack<OnboardingPage>(OnboardingStartPage())
    }

    AnimatedContent(targetState = navigationStack.lastWithIndex()) { (_, page) ->
        when (page) {
            is OnboardingStartPage -> {
                OnboardingStartScreen(
                    onClickStart = {
                        navigationStack.push(OnboardingInformationPage())
                    }
                )
            }

            is OnboardingInformationPage -> {
                OnboardingInformationScreen(
                    onClickContinue = {
                        navigationStack.push(OnboardingTermsPage())
                    }
                )
            }

            is OnboardingTermsPage -> {
                OnboardingTermsScreen(
                    onClickNavigateBack = {
                        navigationStack.back()
                    },
                    onClickReadGeneralTermsAndConditions = {},
                    onClickReadDataProtectionPolicy = {},
                    onClickAccept = {
                        onOnboardingComplete()
                    },
                )
            }
        }
    }
}
