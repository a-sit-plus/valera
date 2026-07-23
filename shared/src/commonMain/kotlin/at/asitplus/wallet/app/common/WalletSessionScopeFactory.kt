package at.asitplus.wallet.app.common

import at.asitplus.wallet.app.common.presentation.LocalPresentmentSessionCoordinator
import data.storage.DataStoreService
import kotlinx.coroutines.cancel
import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatform
import org.multipaz.prompt.PromptModel
import org.multipaz.util.UUID

private fun createWalletSessionScope(
    sessionName: String,
    intentState: IntentState,
    sessionService: SessionService,
    buildContext: BuildContext,
    promptModel: PromptModel,
    platformAdapter: PlatformAdapter,
    dataStoreService: DataStoreService,
): SessionHandle {
    val keystoreService = KeystoreService(dataStoreService)
    val httpService = KoinPlatform.getKoin().get<HttpService>()
    registerCredentialMetadata(buildContext, dataStoreService, httpService)
    val scope = KoinPlatform.getKoin().createScope(
        "$sessionName:${UUID.randomUUID()}",
        named(SESSION_NAME)
    )
    // ErrorService and sessionCoroutineScope are mutually dependent:
    // - sessionCoroutineScope needs ErrorService to report uncaught exceptions
    // - ErrorService needs sessionCoroutineScope to dispatch emissions
    //
    // We break the cycle with a var+lambda: sessionCoroutineScope captures a reference to
    // the var, which is filled in before any coroutine in sessionCoroutineScope can throw.
    // ErrorService is then constructed imperatively from the real scope and registered via
    // scope.declare() instead of letting Koin construct it lazily — that avoids a hidden
    // ordering constraint between scope.declare(WalletSessionBindings) and scope.get<ErrorService>().
    var errorService: ErrorService? = null
    val sessionCoroutineScope = createErrorReportingScope("wallet-session:$sessionName") {
        errorService
    }
    val resolvedErrorService = ErrorService(sessionCoroutineScope)
    errorService = resolvedErrorService
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
    scope.declare(resolvedErrorService)
    // Wire the coordinator (a Koin single) to this session's ErrorService so exceptions in its
    // coroutine scope surface to the user. The lambda is re-evaluated on each exception, so
    // scope.get() throwing after the scope is closed will be caught by createErrorReportingScope
    // and fall back to Napier.
    KoinPlatform.getKoin().get<LocalPresentmentSessionCoordinator>()
        .setErrorServiceProvider { scope.get<ErrorService>() }
    return SessionHandle(scope = scope) {
        sessionCoroutineScope.cancel()
    }
}

/** Creates the persistent main wallet session and immediately triggers DC API credential registration. */
fun createMainWalletSessionScope(
    sessionName: String,
    intentState: IntentState,
    sessionService: SessionService,
    buildContext: BuildContext,
    promptModel: PromptModel,
    platformAdapter: PlatformAdapter,
    dataStoreService: DataStoreService,
): SessionHandle {
    return createWalletSessionScope(
        sessionName = sessionName,
        intentState = intentState,
        sessionService = sessionService,
        buildContext = buildContext,
        promptModel = promptModel,
        platformAdapter = platformAdapter,
        dataStoreService = dataStoreService,
    ).also { sessionHandle ->
        sessionHandle.scope.get<WalletMain>().startDcApiCredentialRegistration()
    }
}

/** Creates a short-lived session for external-flow activities (e.g. presentation or issuance initiated by a third party). Does not register DC API credentials. */
fun createTransientFlowWalletSessionScope(
    sessionName: String,
    intentState: IntentState,
    sessionService: SessionService,
    buildContext: BuildContext,
    promptModel: PromptModel,
    platformAdapter: PlatformAdapter,
    dataStoreService: DataStoreService,
): SessionHandle = createWalletSessionScope(
    sessionName = sessionName,
    intentState = intentState,
    sessionService = sessionService,
    buildContext = buildContext,
    promptModel = promptModel,
    platformAdapter = platformAdapter,
    dataStoreService = dataStoreService,
)
