package at.asitplus.wallet.app.common.presentation

import at.asitplus.wallet.lib.zk.iso.IsoMdocZkBackendRegistry
import at.asitplus.wallet.lib.zk.iso.LongfellowBackend
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Registers the Longfellow-ZK backend in [IsoMdocZkBackendRegistry.Default], which `HolderAgent` uses to answer
 * ISO mdoc ZKP requests. The registry belongs to the process, so on iOS the main app and the Identity Document
 * Provider extension each register their own backend.
 */
object LongfellowZkRegistration {
    private val mutex = Mutex()
    private var registered = false

    /**
     * Loads the Longfellow circuits off the calling thread. A failed registration is logged and retried on the next
     * call; until then, presentations fall back to plain mdocs where the verifier allows it.
     */
    suspend fun ensureRegistered() {
        mutex.withLock {
            if (registered) return
            withContext(Dispatchers.IO) { IsoMdocZkBackendRegistry.Default.register(LongfellowBackend()) }
                .onSuccess { registered = true }
                .onFailure { Napier.w("Unable to register Longfellow-ZK backend", it) }
        }
    }
}
