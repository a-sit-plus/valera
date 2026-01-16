package at.asitplus.wallet.app.android

import MainView
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.BuildType
import at.asitplus.wallet.app.common.IntentState
import io.github.aakira.napier.Napier
import org.multipaz.prompt.AndroidPromptModel
import org.multipaz.prompt.PromptModel


class MainActivity : AbstractWalletActivity() {
    private val intentState = IntentState()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        intentState.finishApp = { finish().also { Napier.v("Finish called") } }
        intentState.isIntentActivity = false

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
        if (isReturnToAppIntent(intent)) {
            intentState.appLink.value = intent.data?.toString()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        populateLink(intent)
        if (shouldForwardToIntentActivity(intent)) {
            // TODO check if this is ever called
            throw RuntimeException("should not be called")
            Napier.e("HIER!!!!!!!!!!!!!!!!!!!!!!!!! shouldForwardToIntentActivity trze")
            startActivity(Intent(intent).setClass(this, IntentActivity::class.java))
        }
    }

    private fun shouldForwardToIntentActivity(intent: Intent): Boolean {
        return !isReturnToAppIntent(intent) && (intent.action != Intent.ACTION_MAIN || intent.data != null)
    }

    private fun isReturnToAppIntent(intent: Intent): Boolean {
        val data = intent.data ?: return false
        return intent.action == Intent.ACTION_VIEW &&
            data.scheme == "asitplus-wallet" &&
            data.host == "wallet.a-sit.at" &&
            data.path?.startsWith("/app") == true
    }
}
