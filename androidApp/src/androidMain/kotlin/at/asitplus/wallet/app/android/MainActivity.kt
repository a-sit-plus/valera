package at.asitplus.wallet.app.android

import BuildContext
import MainView
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import appLink

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appLink.value = intent.data?.toString()
        setContent {
            MainView(
                buildContext = BuildContext(
                    buildType = BuildConfig.BUILD_TYPE,
                    versionCode = BuildConfig.VERSION_CODE,
                    versionName = BuildConfig.VERSION_NAME,
                )
            )
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            appLink.value = intent.data?.toString()
        }
    }
}