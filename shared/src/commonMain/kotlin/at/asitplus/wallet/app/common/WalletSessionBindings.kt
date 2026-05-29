package at.asitplus.wallet.app.common

import data.storage.DataStoreService
import kotlinx.coroutines.CoroutineScope
import org.multipaz.prompt.PromptModel

/**
 * Session-local dependencies supplied by the hosting activity.
 */
data class WalletSessionBindings(
    val intentState: IntentState,
    val sessionService: SessionService,
    val buildContext: BuildContext,
    val promptModel: PromptModel,
    val platformAdapter: PlatformAdapter,
    val dataStoreService: DataStoreService,
    val keystoreService: KeystoreService,
    /** Cancelled via [SessionHandle.onClose] when the session scope is replaced or closed. */
    val sessionCoroutineScope: CoroutineScope,
)
