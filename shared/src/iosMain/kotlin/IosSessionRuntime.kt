import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.IntentState
import at.asitplus.wallet.app.common.SessionHandle
import at.asitplus.wallet.app.common.SessionService
import at.asitplus.wallet.app.common.SnackbarService
import at.asitplus.wallet.app.common.createMainWalletSessionScope
import at.asitplus.wallet.app.common.createSharingWalletSessionScope
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
    SHARING,
}

private object IosSessionRuntime {
    private val stateLock = SynchronizedObject()
    private var isBootstrapped = false
    private var sessionHandle: IosSessionHandle? = null
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
        return synchronized(stateLock) {
            check(isBootstrapped) {
                "IosSessionRuntime must be bootstrapped before creating a session"
            }

            sessionHandle?.let {
                return it
            }

            val intentState = IntentState()
            val promptModel = IosPromptModel.Builder().apply { addCommonDialogs() }.build()
            val sessionService = SessionService().apply {
                initialize(onReset = { onSessionReset(intentState) }) {
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

            IosSessionHandle(
                intentState = intentState,
                sessionService = sessionService,
                promptModel = promptModel
            ).also { handle ->
                sessionHandle = handle
                applyPendingState(handle.intentState)
            }
        }
    }

    // Called when SessionService.newScope() triggers the scope factory (i.e. on app reset).
    // Clears stale pending state and wipes the intentState so navigation recomposes cleanly.
    private fun onSessionReset(intentState: IntentState) {
        synchronized(stateLock) {
            pendingAppLink = null
            pendingPreRequestData = null
            pendingInvocationData = null
            intentState.reset()
        }
    }

    fun handleIncomingUrl(url: String) {
        synchronized(stateLock) {
            sessionHandle?.intentState?.appLink?.value = url
            pendingAppLink = url
        }
    }

    fun registerDcapiInvocation(data: IosDCAPIInvocationData) {
        synchronized(stateLock) {
            pendingPreRequestData = null
            pendingInvocationData = data
            sessionHandle?.intentState?.let { intentState ->
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
            sessionHandle?.intentState?.let { intentState ->
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
            sessionHandle?.intentState?.let { intentState ->
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
            sessionHandle?.intentState?.let { intentState ->
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
        synchronized(stateLock) {
            val snackbarService = sessionHandle?.sessionService?.scope?.value?.get<SnackbarService>()
            if (snackbarService == null) {
                Napier.w("IosSessionRuntime could not resolve SnackbarService for message: $text")
                return
            }
            snackbarService.showSnackbar(text, duration = duration)
        }
    }

    // Called when a new session is first created. Applies any DC API data that arrived before
    // the session existed. finishApp must be set here too — without it the back handler in
    // SharingNavigation would be a no-op, and the extension would never return to the caller
    // on the first request.
    private fun applyPendingState(intentState: IntentState) {
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
            return
        }
        pendingAppLink?.let { link ->
            intentState.appLink.value = link
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

internal fun getOrCreateIosSharingSession(buildContext: BuildContext): Triple<IntentState, SessionService, PromptModel> {
    val session = IosSessionRuntime.getOrCreateSession(buildContext, IosSessionKind.SHARING)
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
        IosSessionKind.SHARING -> createSharingWalletSessionScope(
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
