package at.asitplus.wallet.app.android

import AndroidPlatformAdapter
import android.app.Application
import at.asitplus.wallet.app.common.BuildType
import at.asitplus.wallet.app.common.IntentState
import at.asitplus.wallet.app.common.di.appModule
import data.storage.AntilogAdapter
import io.github.aakira.napier.Napier
import org.koin.core.context.startKoin
import org.koin.dsl.module

class WalletApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeLogging()
        startKoin {
            modules(appModule(), module { single { createAndroidBuildContext() } })
        }
    }

    private fun initializeLogging() {
        val buildType = BuildType.valueOf(BuildConfig.BUILD_TYPE.uppercase())
        Napier.takeLogarithm()
        Napier.base(
            AntilogAdapter(
                platformAdapter = AndroidPlatformAdapter(applicationContext, IntentState()),
                defaultTag = "",
                buildType = buildType
            )
        )
    }

}
