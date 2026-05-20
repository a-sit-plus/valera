package at.asitplus.wallet.app.common

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
    return SessionHandle(scope = scope) {
        sessionCoroutineScope.cancel()
    }
}

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
