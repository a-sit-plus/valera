package at.asitplus.wallet.app.android

import AndroidPlatformAdapter
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.BuildType
import at.asitplus.wallet.app.common.IntentState
import at.asitplus.wallet.app.common.SessionHandle
import at.asitplus.wallet.app.common.SessionService
import at.asitplus.wallet.app.common.createWalletSessionScope
import data.storage.RealDataStoreService
import data.storage.getDataStore
import org.multipaz.prompt.PromptModel

internal fun createBuildContext(): BuildContext =
    BuildContext(
        buildType = BuildType.valueOf(BuildConfig.BUILD_TYPE.uppercase()),
        packageName = BuildConfig.APPLICATION_ID,
        versionCode = BuildConfig.VERSION_CODE,
        versionName = BuildConfig.VERSION_NAME,
        osVersion = "Android ${Build.VERSION.RELEASE}"
    )

internal fun createWalletSessionScope(
    sessionName: String,
    activity: AppCompatActivity,
    intentState: IntentState,
    sessionService: SessionService,
    buildContext: BuildContext,
    promptModel: PromptModel,
): SessionHandle {
    val platformAdapter = AndroidPlatformAdapter(activity, intentState)
    return createWalletSessionScope(
        sessionName = sessionName,
        intentState = intentState,
        sessionService = sessionService,
        buildContext = buildContext,
        promptModel = promptModel,
        platformAdapter = platformAdapter,
        dataStoreService = RealDataStoreService(getDataStore(activity), platformAdapter),
    )
}