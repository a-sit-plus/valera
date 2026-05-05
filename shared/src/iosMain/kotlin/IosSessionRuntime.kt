import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.ErrorService
import at.asitplus.wallet.app.common.IntentState
import at.asitplus.wallet.app.common.KeystoreService
import at.asitplus.wallet.app.common.SESSION_NAME
import at.asitplus.wallet.app.common.SessionHandle
import at.asitplus.wallet.app.common.SessionService
import at.asitplus.wallet.app.common.WalletSessionBindings
import at.asitplus.wallet.app.common.createErrorReportingScope
import at.asitplus.wallet.app.common.di.appModule
import at.asitplus.wallet.app.dcapi.IosDCAPIInvocationData
import data.storage.AntilogAdapter
import data.storage.RealDataStoreService
import data.storage.createDataStore
import io.github.aakira.napier.Napier
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.cancel
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.mp.KoinPlatform
import org.multipaz.prompt.IosPromptModel
import org.multipaz.prompt.PromptModel
import ui.navigation.IntentService.Companion.IOS_DC_API_CALL
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private data class IosSessionHandle(
    val intentState: IntentState,
    val sessionService: SessionService,
    val promptModel: PromptModel,
)

private object IosSessionRuntime {
    private val stateLock = SynchronizedObject()
    private var isBootstrapped = false
    private var sessionHandle: IosSessionHandle? = null
    private var pendingAppLink: String? = null
    private var pendingInvocationData: IosDCAPIInvocationData? = null

    fun bootstrap(buildContext: BuildContext) {
        synchronized(stateLock) {
            if (isBootstrapped) {
                return
            }

            initializeCredentialSchemes()
            initializeLogging(buildContext)
            startKoin {
                modules(appModule())
            }
            isBootstrapped = true
        }
    }

    fun getOrCreateSession(buildContext: BuildContext): IosSessionHandle {
        return synchronized(stateLock) {
            check(isBootstrapped) {
                "IosSessionRuntime must be bootstrapped before creating a session"
            }

            sessionHandle?.let { return it }

            val intentState = IntentState()
            val promptModel = IosPromptModel.Builder().apply { addCommonDialogs() }.build()
            val sessionService = SessionService().apply {
                initialize {
                    createIosWalletSessionScope(
                        sessionName = "ios",
                        sessionService = this,
                        intentState = intentState,
                        buildContext = buildContext,
                        promptModel = promptModel
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

    fun handleIncomingUrl(url: String) {
        synchronized(stateLock) {
            sessionHandle?.intentState?.appLink?.value = url
            pendingAppLink = url
        }
    }

    fun registerDcapiInvocation(data: IosDCAPIInvocationData) {
        synchronized(stateLock) {
            pendingInvocationData = data
            sessionHandle?.intentState?.let { intentState ->
                intentState.dcapiInvocationData.value = data
                intentState.appLink.value = IOS_DC_API_CALL
            }
        }
        Napier.d("IosSessionRuntime registered DCAPI invocation for origin=${data.origin}")
    }

    fun clearDcapiInvocation() {
        synchronized(stateLock) {
            pendingInvocationData = null
            sessionHandle?.intentState?.let { intentState ->
                intentState.dcapiInvocationData.value = null
                if (intentState.appLink.value == IOS_DC_API_CALL) {
                    intentState.appLink.value = null
                }
            }
        }
    }

    private fun applyPendingState(intentState: IntentState) {
        pendingInvocationData?.let { invocation ->
            intentState.dcapiInvocationData.value = invocation
            intentState.appLink.value = IOS_DC_API_CALL
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

    private fun initializeLogging(buildContext: BuildContext) {
        Napier.takeLogarithm()
        Napier.base(
            AntilogAdapter(
                platformAdapter = IosPlatformAdapter(IntentState()),
                defaultTag = "",
                buildType = buildContext.buildType
            )
        )
    }
}

object IosSessionBridge {
    fun bootstrap(buildContext: BuildContext) {
        IosSessionRuntime.bootstrap(buildContext)
    }

    fun handleIncomingUrl(url: String) {
        IosSessionRuntime.handleIncomingUrl(url)
    }

    fun registerDcapiInvocation(data: IosDCAPIInvocationData) {
        IosSessionRuntime.registerDcapiInvocation(data)
    }

    fun clearDcapiInvocation() {
        IosSessionRuntime.clearDcapiInvocation()
    }
}

internal fun getOrCreateIosSession(buildContext: BuildContext): Triple<IntentState, SessionService, PromptModel> {
    val session = IosSessionRuntime.getOrCreateSession(buildContext)
    return Triple(session.intentState, session.sessionService, session.promptModel)
}

@OptIn(ExperimentalUuidApi::class)
private fun createIosWalletSessionScope(
    sessionName: String,
    sessionService: SessionService,
    intentState: IntentState,
    buildContext: BuildContext,
    promptModel: PromptModel,
): SessionHandle {
    val platformAdapter = IosPlatformAdapter(intentState)
    val dataStoreService = RealDataStoreService(createDataStore(), platformAdapter)
    val keystoreService = KeystoreService(dataStoreService)
    val scope = KoinPlatform.getKoin().createScope(
        "$sessionName:${Uuid.random()}",
        named(SESSION_NAME)
    )
    var errorService: ErrorService? = null
    val sessionCoroutineScope = createErrorReportingScope("wallet-session:$sessionName") {
        errorService
    }

    scope.declare(
        WalletSessionBindings(
            intentState = intentState,
            sessionService = sessionService,
            buildContext = buildContext,
            promptModel = promptModel,
            platformAdapter = platformAdapter,
            dataStoreService = dataStoreService,
            keystoreService = keystoreService,
            sessionCoroutineScope = sessionCoroutineScope
        )
    )
    errorService = scope.get()

    return SessionHandle(scope = scope) {
        sessionCoroutineScope.cancel()
    }
}
