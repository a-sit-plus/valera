package at.asitplus.wallet.app.android

import AndroidPlatformAdapter
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.BuildType
import at.asitplus.wallet.app.common.IntentState
import at.asitplus.wallet.app.common.KeystoreService
import at.asitplus.wallet.app.common.SESSION_NAME
import at.asitplus.wallet.app.common.SessionService
import at.asitplus.wallet.app.common.WalletSessionBindings
import data.storage.RealDataStoreService
import data.storage.getDataStore
import org.koin.core.Koin
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.multipaz.prompt.PromptModel
import java.util.UUID

internal fun createBuildContext(): BuildContext =
    BuildContext(
        buildType = BuildType.valueOf(BuildConfig.BUILD_TYPE.uppercase()),
        packageName = BuildConfig.APPLICATION_ID,
        versionCode = BuildConfig.VERSION_CODE,
        versionName = BuildConfig.VERSION_NAME,
        osVersion = "Android ${Build.VERSION.RELEASE}"
    )

internal fun createWalletSessionScope(
    koin: Koin,
    sessionName: String,
    activity: AppCompatActivity,
    intentState: IntentState,
    sessionService: SessionService,
    buildContext: BuildContext,
    promptModel: PromptModel,
): Scope {
    val platformAdapter = AndroidPlatformAdapter(activity, intentState)
    val dataStoreService = RealDataStoreService(
        getDataStore(activity),
        platformAdapter
    )
    val keystoreService = KeystoreService(dataStoreService)
    val scope = koin.createScope(
        "$sessionName:${UUID.randomUUID()}",
        named(SESSION_NAME)
    )

    scope.declare(
        WalletSessionBindings(
            intentState = intentState,
            sessionService = sessionService,
            buildContext = buildContext,
            promptModel = promptModel,
            platformAdapter = platformAdapter,
            dataStoreService = dataStoreService,
            keystoreService = keystoreService
        )
    )

    return scope
}
