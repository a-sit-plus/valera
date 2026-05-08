package at.asitplus.wallet.app.android

import AndroidPlatformAdapter
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.BuildType
import at.asitplus.wallet.app.common.IntentState
import at.asitplus.wallet.app.common.SessionHandle
import at.asitplus.wallet.app.common.SessionService
import at.asitplus.wallet.app.common.createMainWalletSessionScope as createSharedMainWalletSessionScope
import at.asitplus.wallet.app.common.createSharingWalletSessionScope as createSharedSharingWalletSessionScope
import data.storage.RealDataStoreService
import data.storage.getDataStore
import org.multipaz.prompt.PromptModel

internal fun createAndroidBuildContext(): BuildContext =
    BuildContext(
        buildType = BuildType.valueOf(BuildConfig.BUILD_TYPE.uppercase()),
        packageName = BuildConfig.APPLICATION_ID,
        versionCode = BuildConfig.VERSION_CODE,
        versionName = BuildConfig.VERSION_NAME,
        osVersion = "Android ${Build.VERSION.RELEASE}",
    )

internal fun createAndroidMainWalletSessionScope(
    sessionName: String,
    activity: AppCompatActivity,
    intentState: IntentState,
    sessionService: SessionService,
    buildContext: BuildContext,
    promptModel: PromptModel,
): SessionHandle {
    val platformAdapter = AndroidPlatformAdapter(activity, intentState)
    return createSharedMainWalletSessionScope(
        sessionName = sessionName,
        intentState = intentState,
        sessionService = sessionService,
        buildContext = buildContext,
        promptModel = promptModel,
        platformAdapter = platformAdapter,
        dataStoreService = RealDataStoreService(getDataStore(activity), platformAdapter),
    )
}

internal fun createAndroidSharingWalletSessionScope(
    sessionName: String,
    activity: AppCompatActivity,
    intentState: IntentState,
    sessionService: SessionService,
    buildContext: BuildContext,
    promptModel: PromptModel,
): SessionHandle {
    val platformAdapter = AndroidPlatformAdapter(activity, intentState)
    return createSharedSharingWalletSessionScope(
        sessionName = sessionName,
        intentState = intentState,
        sessionService = sessionService,
        buildContext = buildContext,
        promptModel = promptModel,
        platformAdapter = platformAdapter,
        dataStoreService = RealDataStoreService(getDataStore(activity), platformAdapter),
    )
}
