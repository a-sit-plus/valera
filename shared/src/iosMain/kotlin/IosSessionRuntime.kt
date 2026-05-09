import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.IntentState
import at.asitplus.wallet.app.common.SessionHandle
import at.asitplus.wallet.app.common.SessionService
import at.asitplus.wallet.app.common.SnackbarService
import at.asitplus.wallet.app.common.createMainWalletSessionScope
import at.asitplus.wallet.app.common.createTransientFlowWalletSessionScope
import at.asitplus.wallet.app.common.di.appModule
import at.asitplus.wallet.app.dcapi.IosDcApiPreRequestData
import at.asitplus.wallet.app.dcapi.IosDCAPIInvocationData
import androidx.compose.material3.SnackbarDuration
import data.storage.RealDataStoreService
import io.github.aakira.napier.Antilog
import data.storage.createDataStore
import io.github.aakira.napier.Napier
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import org.koin.core.context.startKoin
import org.multipaz.prompt.IosPromptModel
import org.multipaz.prompt.PromptModel
import ui.navigation.IntentService.Companion.IOS_DC_API_CALL
import ui.navigation.IntentService.Companion.IOS_DC_API_PRE_REQUEST

private data class IosSessionHandle(
    val intentState: IntentState,
    val sessionService: SessionService,
    val promptModel: PromptModel,
)

private enum class IosSessionKind {
    MAIN,
    TRANSIENT_FLOW,
}

private object IosSessionRuntime {
    private val stateLock = SynchronizedObject()
    private var isBootstrapped = false
    // Separate handles per session kind so MAIN and TRANSIENT_FLOW never share state.
    private var mainHandle: IosSessionHandle? = null
    private var transientFlowHandle: IosSessionHandle? = null
    private var pendingAppLink: String? = null
    private var pendingPreRequestData: IosDcApiPreRequestData? = null
    private var pendingInvocationData: IosDCAPIInvocationData? = null

    fun bootstrap(buildContext: BuildContext, antilog: Antilog) {
        synchronized(stateLock) {
            if (isBootstrapped) {
                return
            }

            initializeCredentialSchemes()
            initializeLogging(antilog)
            startKoin {
                modules(appModule())
            }
            isBootstrapped = true
        }
    }

    fun getOrCreateSession(buildContext: BuildContext, sessionKind: IosSessionKind): IosSessionHandle {
        // Fast path: return existing handle without creating a session.
        synchronized(stateLock) {
            check(isBootstrapped) { "IosSessionRuntime must be bootstrapped before creating a session" }
            handleFor(sessionKind)?.let { return it }
        }

        // Slow path: create outside the lock to avoid holding stateLock during Koin scope
        // creation, which uses its own internal synchronisation and could cause a deadlock.
        val intentState = IntentState()
        val promptModel = IosPromptModel.Builder().apply { addCommonDialogs() }.build()
        val sessionService = SessionService().apply {
            initialize(onReset = { onSessionReset(sessionKind, intentState) }) {
                createIosWalletSessionScope(
                    sessionName = "ios",
                    sessionService = this,
                    intentState = intentState,
                    buildContext = buildContext,
                    promptModel = promptModel,
                    sessionKind = sessionKind,
                )
            }
        }
        val newHandle = IosSessionHandle(intentState, sessionService, promptModel)

        // Re-acquire lock to store; if another thread created the same kind in the meantime,
        // discard our new handle and return the one that won the race.
        return synchronized(stateLock) {
            handleFor(sessionKind)?.also {
                newHandle.sessionService.close()
            } ?: newHandle.also { handle ->
                setHandle(sessionKind, handle)
                applyPendingState(sessionKind, handle.intentState)
            }
        }
    }

    private fun handleFor(sessionKind: IosSessionKind): IosSessionHandle? = when (sessionKind) {
        IosSessionKind.MAIN -> mainHandle
        IosSessionKind.TRANSIENT_FLOW -> transientFlowHandle
    }

    private fun setHandle(sessionKind: IosSessionKind, handle: IosSessionHandle) {
        when (sessionKind) {
            IosSessionKind.MAIN -> mainHandle = handle
            IosSessionKind.TRANSIENT_FLOW -> transientFlowHandle = handle
        }
    }

    // Called when SessionService.newScope() triggers the scope factory (i.e. on app reset).
    // Clears stale pending state and wipes the intentState so navigation recomposes cleanly.
    private fun onSessionReset(sessionKind: IosSessionKind, intentState: IntentState) {
        synchronized(stateLock) {
            when (sessionKind) {
                IosSessionKind.MAIN -> pendingAppLink = null
                IosSessionKind.TRANSIENT_FLOW -> {
                    pendingPreRequestData = null
                    pendingInvocationData = null
                }
            }
            intentState.reset()
        }
    }

    fun handleIncomingUrl(url: String) {
        synchronized(stateLock) {
            if (pendingAppLink != null && mainHandle == null) {
                Napier.w("IosSessionRuntime: overwriting pending app link '$pendingAppLink' with '$url'")
            }
            mainHandle?.intentState?.appLink?.value = url
            pendingAppLink = url
        }
    }

    fun registerDcapiInvocation(data: IosDCAPIInvocationData) {
        synchronized(stateLock) {
            pendingPreRequestData = null
            pendingInvocationData = data
            transientFlowHandle?.intentState?.let { intentState ->
                intentState.iosDcApiPreRequestData.value = null
                intentState.dcapiInvocationData.value = data
                intentState.appLink.value = IOS_DC_API_CALL
                intentState.finishApp = { data.onCancel() }
            }
        }
        Napier.d("IosSessionRuntime registered DCAPI invocation for origin=${data.origin}")
    }

    fun registerDcapiPreRequest(data: IosDcApiPreRequestData) {
        synchronized(stateLock) {
            pendingInvocationData = null
            pendingPreRequestData = data
            transientFlowHandle?.intentState?.let { intentState ->
                intentState.dcapiInvocationData.value = null
                intentState.iosDcApiPreRequestData.value = data
                intentState.appLink.value = IOS_DC_API_PRE_REQUEST
                intentState.finishApp = { data.onCancel() }
            }
        }
        Napier.d("IosSessionRuntime registered DCAPI pre-request for origin=${data.origin}")
    }

    fun clearDcapiInvocation() {
        synchronized(stateLock) {
            pendingInvocationData = null
            transientFlowHandle?.intentState?.let { intentState ->
                intentState.dcapiInvocationData.value = null
                intentState.finishApp = null
                if (intentState.appLink.value == IOS_DC_API_CALL) {
                    intentState.appLink.value = null
                }
            }
        }
    }

    fun clearDcapiPreRequest() {
        synchronized(stateLock) {
            pendingPreRequestData = null
            transientFlowHandle?.intentState?.let { intentState ->
                intentState.iosDcApiPreRequestData.value = null
                if (intentState.appLink.value == IOS_DC_API_PRE_REQUEST) {
                    intentState.appLink.value = null
                }
                if (intentState.dcapiInvocationData.value == null) {
                    intentState.finishApp = null
                }
            }
        }
    }

    fun showSnackbar(text: String, duration: SnackbarDuration = SnackbarDuration.Short) {
        // Resolve the service under the lock, then dispatch outside it to avoid holding
        // stateLock across a coroutine launch (L-6).
        val snackbarService = synchronized(stateLock) {
            mainHandle?.sessionService?.scope?.value?.get<SnackbarService>()
                ?: transientFlowHandle?.sessionService?.scope?.value?.get<SnackbarService>()
        }
        if (snackbarService == null) {
            Napier.w("IosSessionRuntime could not resolve SnackbarService for message: $text")
            return
        }
        snackbarService.showSnackbar(text, duration = duration)
    }

    // Called when a new session is first created. Applies pending state that arrived before
    // the session existed. DC API data goes to the TRANSIENT_FLOW session; URL links go to MAIN.
    // finishApp must be set here too — without it the back handler in TransientFlowNavigation
    // would be a no-op and the extension would never return to the caller on the first request.
    private fun applyPendingState(sessionKind: IosSessionKind, intentState: IntentState) {
        when (sessionKind) {
            IosSessionKind.MAIN -> {
                pendingAppLink?.let { link ->
                    intentState.appLink.value = link
                }
            }
            IosSessionKind.TRANSIENT_FLOW -> {
                pendingPreRequestData?.let { preRequest ->
                    intentState.iosDcApiPreRequestData.value = preRequest
                    intentState.appLink.value = IOS_DC_API_PRE_REQUEST
                    intentState.finishApp = { preRequest.onCancel() }
                    return
                }
                pendingInvocationData?.let { invocation ->
                    intentState.dcapiInvocationData.value = invocation
                    intentState.appLink.value = IOS_DC_API_CALL
                    intentState.finishApp = { invocation.onCancel() }
                }
            }
        }
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

    private fun initializeLogging(antilog: Antilog) {
        Napier.takeLogarithm()
        Napier.base(antilog)
    }
}

object IosSessionBridge {
    fun bootstrap(buildContext: BuildContext, antilog: Antilog) {
        IosSessionRuntime.bootstrap(buildContext, antilog)
    }

    fun handleIncomingUrl(url: String) {
        IosSessionRuntime.handleIncomingUrl(url)
    }

    fun registerDcapiInvocation(data: IosDCAPIInvocationData) {
        IosSessionRuntime.registerDcapiInvocation(data)
    }

    fun registerDcapiPreRequest(data: IosDcApiPreRequestData) {
        IosSessionRuntime.registerDcapiPreRequest(data)
    }

    fun clearDcapiInvocation() {
        IosSessionRuntime.clearDcapiInvocation()
    }

    fun clearDcapiPreRequest() {
        IosSessionRuntime.clearDcapiPreRequest()
    }

    fun showSnackbar(text: String, duration: SnackbarDuration = SnackbarDuration.Short) {
        IosSessionRuntime.showSnackbar(text, duration)
    }
}

internal fun getOrCreateIosSession(buildContext: BuildContext): Triple<IntentState, SessionService, PromptModel> {
    val session = IosSessionRuntime.getOrCreateSession(buildContext, IosSessionKind.MAIN)
    return Triple(session.intentState, session.sessionService, session.promptModel)
}

internal fun getOrCreateIosTransientFlowSession(buildContext: BuildContext): Triple<IntentState, SessionService, PromptModel> {
    val session = IosSessionRuntime.getOrCreateSession(buildContext, IosSessionKind.TRANSIENT_FLOW)
    return Triple(session.intentState, session.sessionService, session.promptModel)
}

private fun createIosWalletSessionScope(
    sessionName: String,
    sessionService: SessionService,
    intentState: IntentState,
    buildContext: BuildContext,
    promptModel: PromptModel,
    sessionKind: IosSessionKind,
): SessionHandle {
    val platformAdapter = IosPlatformAdapter(intentState)
    val dataStoreService = RealDataStoreService(createDataStore(), platformAdapter)
    return when (sessionKind) {
        IosSessionKind.MAIN -> createMainWalletSessionScope(
            sessionName = sessionName,
            intentState = intentState,
            sessionService = sessionService,
            buildContext = buildContext,
            promptModel = promptModel,
            platformAdapter = platformAdapter,
            dataStoreService = dataStoreService,
        )
        IosSessionKind.TRANSIENT_FLOW -> createTransientFlowWalletSessionScope(
            sessionName = sessionName,
            intentState = intentState,
            sessionService = sessionService,
            buildContext = buildContext,
            promptModel = promptModel,
            platformAdapter = platformAdapter,
            dataStoreService = dataStoreService,
        )
    }
}
