package at.asitplus.wallet.app.android

import AndroidPlatformAdapter
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import at.asitplus.wallet.app.common.IntentState
import at.asitplus.wallet.app.common.SessionService
import org.multipaz.prompt.AndroidPromptModel
import org.multipaz.prompt.PromptModel
import java.lang.ref.WeakReference

internal enum class AndroidWalletSessionKind {
    MAIN,
    TRANSIENT_FLOW,
}

/**
 * Owns an Activity's wallet session across configuration changes. Activity-only operations are
 * routed through [activityHost], while the retained service graph only owns application-scoped
 * objects.
 */
internal class AndroidWalletSessionViewModel(
    context: Context,
    sessionKind: AndroidWalletSessionKind,
) : ViewModel() {
    private val applicationContext = context.applicationContext
    private val activityHost = AndroidActivityHost()
    private var initialIntentConsumed = false

    val intentState = IntentState().apply {
        finishApp = activityHost::finishCurrentActivity
    }
    val buildContext = createAndroidBuildContext()
    val promptModel: PromptModel =
        AndroidPromptModel.Builder().apply { addCommonDialogs() }.build()
    private val platformAdapter = AndroidPlatformAdapter(applicationContext, intentState)
    val sessionService = SessionService().apply {
        initialize {
            when (sessionKind) {
                AndroidWalletSessionKind.MAIN -> createAndroidMainWalletSessionScope(
                    sessionName = "main",
                    context = applicationContext,
                    intentState = intentState,
                    sessionService = this,
                    buildContext = buildContext,
                    promptModel = promptModel,
                    platformAdapter = platformAdapter,
                )

                AndroidWalletSessionKind.TRANSIENT_FLOW -> createAndroidTransientFlowWalletSessionScope(
                    sessionName = "transientFlow",
                    context = applicationContext,
                    intentState = intentState,
                    sessionService = this,
                    buildContext = buildContext,
                    promptModel = promptModel,
                    platformAdapter = platformAdapter,
                )
            }
        }
    }

    fun attach(activity: AbstractWalletActivity) {
        activityHost.attach(activity)
    }

    fun detach(activity: AbstractWalletActivity) {
        activityHost.detach(activity)
    }

    /** Returns false when Android is merely recreating the Activity with its existing Intent. */
    fun consumeInitialIntent(): Boolean {
        if (initialIntentConsumed) return false
        initialIntentConsumed = true
        return true
    }

    fun sendCredentialResponseToInvoker(result: String, success: Boolean) {
        activityHost.dispatch { activity ->
            activity.sendCredentialResponseToDCAPIInvoker(result, success)
        }
    }

    fun sendCredentialCreationResponseToInvoker(result: String, success: Boolean) {
        activityHost.dispatch { activity ->
            activity.sendCredentialCreationResponseToDCAPIInvoker(result, success)
        }
    }

    override fun onCleared() {
        sessionService.close()
        activityHost.clear()
    }

    companion object {
        fun factory(
            context: Context,
            sessionKind: AndroidWalletSessionKind,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(AndroidWalletSessionViewModel::class.java))
                return AndroidWalletSessionViewModel(context, sessionKind) as T
            }
        }
    }
}

/**
 * Provides the current Activity without retaining destroyed instances. Work completed during the
 * configuration-change gap is queued and delivered to the replacement Activity on attachment.
 */
private class AndroidActivityHost {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var activityReference: WeakReference<AbstractWalletActivity>? = null
    private val pendingActions = ArrayDeque<(AbstractWalletActivity) -> Unit>()
    private var cleared = false

    fun attach(activity: AbstractWalletActivity) = runOnMain {
        if (cleared) return@runOnMain
        activityReference = WeakReference(activity)
        val actions = pendingActions.toList()
        pendingActions.clear()
        actions.forEach { action -> action(activity) }
    }

    fun detach(activity: AbstractWalletActivity) = runOnMain {
        if (activityReference?.get() === activity) {
            activityReference = null
        }
    }

    fun dispatch(action: (AbstractWalletActivity) -> Unit) = runOnMain {
        if (cleared) return@runOnMain
        activityReference?.get()?.let(action) ?: pendingActions.addLast(action)
    }

    fun finishCurrentActivity() = dispatch { it.finish() }

    fun clear() = runOnMain {
        cleared = true
        activityReference = null
        pendingActions.clear()
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }
}
