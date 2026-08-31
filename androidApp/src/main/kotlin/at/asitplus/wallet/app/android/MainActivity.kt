package at.asitplus.wallet.app.android

import MainView
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import io.github.aakira.napier.Napier
import ui.navigation.IntentService.Companion.PRESENTATION_REQUESTED_INTENT

class MainActivity : AbstractWalletActivity() {
    private val sessionViewModel: AndroidWalletSessionViewModel by lazy {
        ViewModelProvider(
            this,
            AndroidWalletSessionViewModel.factory(
                applicationContext,
                AndroidWalletSessionKind.MAIN,
            ),
        )[AndroidWalletSessionViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        sessionViewModel.attach(this)

        setContent {
            MainView(
                buildContext = sessionViewModel.buildContext,
                promptModel = sessionViewModel.promptModel,
                intentState = sessionViewModel.intentState,
                sessionService = sessionViewModel.sessionService,
            )
        }
    }

    override fun populateLink(intent: Intent) {
        if (!sessionViewModel.consumeInitialIntent()) return
        applyIntent(intent)
    }

    private fun applyIntent(intent: Intent) {
        Napier.d("MainActivity.populateLink url=${intent.data} action=${intent.action}")
        when (intent.action) {
            PRESENTATION_REQUESTED_INTENT -> {
                Napier.d("MainActivity PRESENTATION_REQUESTED_INTENT")
                sessionViewModel.intentState.presentationStateModelProvider = {
                    NdefDeviceEngagementService.currentPresentationStateModel
                }
                sessionViewModel.intentState.presentationStateModel.value =
                    NdefDeviceEngagementService.currentPresentationStateModel
                sessionViewModel.intentState.appLink.value = PRESENTATION_REQUESTED_INTENT
            }

            else -> {
                Napier.d("MainActivity appLink=${intent.data}")
                sessionViewModel.intentState.presentationStateModel.value = null
                sessionViewModel.intentState.presentationStateModelProvider = null
                sessionViewModel.intentState.appLink.value = intent.data?.toString()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        applyIntent(intent)
    }

    override fun onDestroy() {
        sessionViewModel.detach(this)
        super.onDestroy()
    }
}
