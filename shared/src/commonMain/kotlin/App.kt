import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import at.asitplus.wallet.app.common.WalletMain
import navigation.AboutPage
import navigation.CredentialPage
import navigation.HomePage
import navigation.NavigationStack
import navigation.Page
import org.jetbrains.compose.resources.ExperimentalResourceApi
import ui.theme.WalletTheme
import view.AboutScreen
import view.CredentialScreen
import view.HomeScreen

@OptIn(ExperimentalResourceApi::class)
@Composable
fun App(walletMain: WalletMain) {
    WalletTheme {
        nav()
    }
}

var globalBack: () -> Unit = {}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun nav() {
    // Modified from https://github.com/JetBrains/compose-multiplatform/tree/master/examples/imageviewer

    val navigationStack = rememberSaveable(
        saver = listSaver<NavigationStack<Page>, Page>(
            restore = { NavigationStack(*it.toTypedArray()) },
            save = { it.stack },
        )
    ) {
        NavigationStack(HomePage())
    }

    globalBack = { navigationStack.back() }

    AnimatedContent(targetState = navigationStack.lastWithIndex()) { (_, page) ->
        when (page) {
            is HomePage -> {
                HomeScreen(onAbout = { navigationStack.push(AboutPage()) }, onCredential = {navigationStack.push(CredentialPage())})
            }
            is AboutPage -> {
                AboutScreen()
            }
            is CredentialPage -> {
                CredentialScreen()
            }
        }
    }
}

expect fun getPlatformName(): String

@Composable
expect fun getColorScheme(): ColorScheme