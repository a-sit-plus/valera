package at.asitplus.wallet.app.android

import MainView
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import at.asitplus.wallet.app.common.IntentState
import at.asitplus.wallet.app.common.SessionService
import io.github.aakira.napier.Napier
import org.koin.core.context.GlobalContext
import org.multipaz.prompt.AndroidPromptModel
import org.multipaz.prompt.PromptModel


class MainActivity : AbstractWalletActivity() {
    private val intentState = IntentState()
    private val buildContext by lazy { createBuildContext() }
    private val promptModel: PromptModel by lazy {
        AndroidPromptModel.Builder().apply { addCommonDialogs() }.build()
    }
    private lateinit var sessionService: SessionService

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        intentState.finishApp = { finish() }

        if (!::sessionService.isInitialized) {
            sessionService = SessionService().apply {
                initialize {
                    createWalletSessionScope(
                        koin = GlobalContext.get(),
                        sessionName = "main",
                        activity = this@MainActivity,
                        intentState = intentState,
                        sessionService = this,
                        buildContext = buildContext,
                        promptModel = promptModel
                    )
                }
            }
        }

        setContent {
            MainView(
                buildContext = buildContext,
                promptModel = promptModel,
                intentState = intentState,
                sessionService = sessionService
            )
        }
    }

    override fun populateLink(intent: Intent) {
        Napier.d("MainActivity.populateLink url=${intent.data} action=${intent.action}")
        intentState.appLink.value = intent.data?.toString()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        populateLink(intent)
    }

    override fun onDestroy() {
        if (::sessionService.isInitialized && isFinishing) {
            sessionService.close()
        }
        super.onDestroy()
    }
}
