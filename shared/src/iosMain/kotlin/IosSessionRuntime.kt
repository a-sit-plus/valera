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
import org.koin.dsl.module
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
    // All pending state routes to the TRANSIENT_FLOW session — URL flows open as a transient
    // sheet (UrlLink) mirroring Android's TransientFlowActivity; DC API flows use the same path.
    // MAIN has no pending state.
    private sealed class PendingTransientState {
        data class UrlLink(val url: String, val onFinish: () -> Unit) : PendingTransientState()
        data class PreRequest(val data: IosDcApiPreRequestData) : PendingTransientState()
        data class Invocation(val data: IosDCAPIInvocationData) : PendingTransientState()
    }
    private var pendingTransientState: PendingTransientState? = null

    fun bootstrap(buildContext: BuildContext, antilog: Antilog) {
        synchronized(stateLock) {
            if (isBootstrapped) {
                return
            }

            initializeCredentialSchemes()
            initializeLogging(antilog)
            startKoin {
                modules(appModule(), module { single { buildContext } })
            }
            isBootstrapped = true
        }
    }

    fun getOrCreateSession(buildContext: BuildContext, sessionKind: IosSessionKind): IosSessionHandle {
        // Fast path: return the existing handle for this kind without any allocation.
        // A handle whose scope is already closed (e.g. left by the loser of a prior creation
        // race) is treated as absent: clear it and fall through to create a fresh session.
        synchronized(stateLock) {
            check(isBootstrapped) { "IosSessionRuntime must be bootstrapped before creating a session" }
            handleFor(sessionKind)?.let { handle ->
                if (!handle.sessionService.scope.value.closed) return handle
                setHandle(sessionKind, null)
            }
        }

        // Slow path: create the session OUTSIDE stateLock.
        // Koin's createScope() acquires its own internal lock. If stateLock were held here,
        // any Koin module that touches IosSessionBridge during scope initialisation would
        // attempt to acquire stateLock again → lock-ordering inversion → deadlock.
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

        // Re-acquire the lock to store the handle. If another thread created the same kind
        // concurrently, discard our new handle and return the winner to keep the singleton invariant.
        // Close the loser OUTSIDE the lock: SessionService.close() closes the Koin scope, which
        // acquires Koin's internal lock — same ordering constraint as scope creation above.
        var loserHandle: IosSessionHandle? = null
        val winner = synchronized(stateLock) {
            handleFor(sessionKind)?.also {
                loserHandle = newHandle
            } ?: newHandle.also { handle ->
                setHandle(sessionKind, handle)
                applyPendingState(sessionKind, handle.intentState)
            }
        }
        loserHandle?.sessionService?.close()
        return winner
    }

    private fun handleFor(sessionKind: IosSessionKind): IosSessionHandle? = when (sessionKind) {
        IosSessionKind.MAIN -> mainHandle
        IosSessionKind.TRANSIENT_FLOW -> transientFlowHandle
    }

    private fun setHandle(sessionKind: IosSessionKind, handle: IosSessionHandle?) {
        when (sessionKind) {
            IosSessionKind.MAIN -> mainHandle = handle
            IosSessionKind.TRANSIENT_FLOW -> transientFlowHandle = handle
        }
    }

    // Called when SessionService.newScope() triggers the scope factory (i.e. on app reset).
    // Clears stale pending state and wipes the intentState so navigation recomposes cleanly.
    private fun onSessionReset(sessionKind: IosSessionKind, intentState: IntentState) {
        synchronized(stateLock) {
            if (sessionKind == IosSessionKind.TRANSIENT_FLOW) pendingTransientState = null
            intentState.reset()
        }
    }

    // onFinish is the Swift callback to dismiss the transient sheet once the flow completes;
    // stored as finishApp so TransientFlowNavigation's back handler can return to the main app.
    fun handleIncomingUrl(url: String, onFinish: () -> Unit) {
        synchronized(stateLock) {
            if (pendingTransientState != null && transientFlowHandle == null) {
                Napier.w("IosSessionRuntime: overwriting pending transient state with UrlLink($url)")
            }
            pendingTransientState = PendingTransientState.UrlLink(url, onFinish)
            transientFlowHandle?.intentState?.let { intentState ->
                intentState.appLink.value = url
                intentState.finishApp = onFinish
            }
        }
    }

    fun registerDcapiInvocation(data: IosDCAPIInvocationData) {
        synchronized(stateLock) {
            pendingTransientState = PendingTransientState.Invocation(data)
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
            pendingTransientState = PendingTransientState.PreRequest(data)
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
            if (pendingTransientState is PendingTransientState.Invocation) pendingTransientState = null
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
            if (pendingTransientState is PendingTransientState.PreRequest) pendingTransientState = null
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
    // the session existed. All pending state belongs to the TRANSIENT_FLOW session: URL flows
    // open a transient sheet and DC API flows use the same path.
    // finishApp must be set here too — without it the back handler in TransientFlowNavigation
    // would be a no-op and the sheet/extension would never return to the caller on the first request.
    private fun applyPendingState(sessionKind: IosSessionKind, intentState: IntentState) {
        if (sessionKind != IosSessionKind.TRANSIENT_FLOW) return
        when (val state = pendingTransientState) {
            is PendingTransientState.UrlLink -> {
                intentState.appLink.value = state.url
                intentState.finishApp = state.onFinish
            }
            is PendingTransientState.PreRequest -> {
                intentState.iosDcApiPreRequestData.value = state.data
                intentState.appLink.value = IOS_DC_API_PRE_REQUEST
                intentState.finishApp = { state.data.onCancel() }
            }
            is PendingTransientState.Invocation -> {
                intentState.dcapiInvocationData.value = state.data
                intentState.appLink.value = IOS_DC_API_CALL
                intentState.finishApp = { state.data.onCancel() }
            }
            null -> {}
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

    fun handleIncomingUrl(url: String, onFinish: () -> Unit) {
        IosSessionRuntime.handleIncomingUrl(url, onFinish)
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
