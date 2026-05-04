package at.asitplus.wallet.app.android

import AndroidPlatformAdapter
import android.app.Application
import at.asitplus.wallet.app.common.BuildType
import at.asitplus.wallet.app.common.IntentState
import at.asitplus.wallet.app.common.di.appModule
import data.storage.AntilogAdapter
import io.github.aakira.napier.Napier
import org.koin.core.context.startKoin

class WalletApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        initializeCredentialSchemes()
        initializeLogging()

        startKoin {
            modules(appModule())
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

    private fun initializeCredentialSchemes() {
        at.asitplus.wallet.mdl.Initializer.initWithVCK()
        at.asitplus.wallet.eupid.Initializer.initWithVCK()
        at.asitplus.wallet.eupidsdjwt.Initializer.initWithVCK()
        at.asitplus.wallet.cor.Initializer.initWithVCK()
        at.asitplus.wallet.por.Initializer.initWithVCK()
        at.asitplus.wallet.companyregistration.Initializer.initWithVCK()
        at.asitplus.wallet.healthid.Initializer.initWithVCK()
        at.asitplus.wallet.taxid.Initializer.initWithVCK()
        at.asitplus.wallet.ehic.Initializer.initWithVCK()
        at.asitplus.wallet.ageverification.Initializer.initWithVCK()
    }
}
