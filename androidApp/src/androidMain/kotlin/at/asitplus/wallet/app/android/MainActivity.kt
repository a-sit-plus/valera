package at.asitplus.wallet.app.android

import MainView
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.BuildType
import io.github.aakira.napier.Napier
import org.multipaz.prompt.AndroidPromptModel
import org.multipaz.prompt.PromptModel


class MainActivity : AbstractWalletActivity() {
    private val intentState = AndroidIntentStateHolder.intentState

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        intentState.finishApp = { finish() }

        val promptModel: PromptModel by lazy {
            AndroidPromptModel.Builder().apply { addCommonDialogs() }.build()
        }

        setContent {
            MainView(
                buildContext = BuildContext(
                    buildType = BuildType.valueOf(BuildConfig.BUILD_TYPE.uppercase()),
                    packageName = BuildConfig.APPLICATION_ID,
                    versionCode = BuildConfig.VERSION_CODE,
                    versionName = BuildConfig.VERSION_NAME,
                    osVersion = "Android ${Build.VERSION.RELEASE}"
                ),
                promptModel,
                intentState = intentState
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
}
