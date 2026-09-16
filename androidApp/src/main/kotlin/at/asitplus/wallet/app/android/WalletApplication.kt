package at.asitplus.wallet.app.android

import AndroidPlatformAdapter
import android.app.Application
import at.asitplus.wallet.app.common.BuildType
import at.asitplus.wallet.app.common.IntentState
import at.asitplus.wallet.app.common.di.appModule
import data.storage.AntilogAdapter
import io.github.aakira.napier.Napier
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.multipaz.context.initializeApplication
import java.security.Security

class WalletApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Complete process-wide crypto setup before any wallet services or HTTP clients start.
        initializeMultipaz()
        initializeLogging()
        startKoin {
            modules(appModule(), module { single { createAndroidBuildContext() } })
        }
    }

    private fun initializeMultipaz() {
        // Multipaz needs the bundled BC provider rather than Android's built-in version.
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) !is BouncyCastleProvider) {
            // Construct first so provider initialization does not extend the gap without BC.
            val provider = BouncyCastleProvider()
            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
            Security.addProvider(provider)
        }
        initializeApplication(applicationContext)
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
