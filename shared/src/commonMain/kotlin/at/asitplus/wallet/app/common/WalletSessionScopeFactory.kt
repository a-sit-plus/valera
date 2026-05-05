package at.asitplus.wallet.app.common

import data.storage.DataStoreService
import kotlinx.coroutines.cancel
import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatform
import org.multipaz.prompt.PromptModel
import org.multipaz.util.UUID

fun createWalletSessionScope(
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