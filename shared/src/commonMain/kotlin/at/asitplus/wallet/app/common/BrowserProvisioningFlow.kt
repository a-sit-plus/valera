package at.asitplus.wallet.app.common

import at.asitplus.wallet.lib.utils.MapStore
import data.storage.PersistentMapStore
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.multipaz.util.UUID

internal class BrowserProvisioningInProgressException :
    IllegalStateException("Finish the credential issuance in the browser first")

/** Coordinates browser issuance across sessions through the persisted, expiring OAuth state. */
internal class BrowserProvisioningFlow(
    private val activeStates: PersistentMapStore<String, String>,
    private val contexts: MapStore<String, String>,
    private val attestations: MapStore<String, String>,
    private val codeVerifiers: MapStore<String, String>,
) {
    suspend fun isActive(): Boolean = activeStates.get(ACTIVE_PROVISIONING_STATE_KEY) != null

    suspend fun <T> start(
        prepare: suspend () -> T,
        stateOf: (T) -> String,
        persist: suspend (T) -> Unit,
        open: (T) -> Unit,
    ): T {
        val reservation = "$STARTING_PREFIX${UUID.randomUUID()}"
        var state: String? = null
        try {
            if (!transition(expected = null, next = reservation)) throw BrowserProvisioningInProgressException()
            val request = prepare()
            val requestState = stateOf(request)
            state = requestState
            persist(request)
            check(transition(expected = reservation, next = requestState)) {
                "The browser provisioning reservation expired before authorization started"
            }
            open(request)
            return request
        } catch (e: Throwable) {
            withContext(NonCancellable) {
                try {
                    state?.let { cleanUp(it, owner = it) }
                } finally {
                    transition(expected = reservation, next = null)
                }
            }
            throw e
        }
    }

    suspend fun <T> resume(state: String, handle: suspend (context: String, attestation: String?) -> T): T {
        require(!state.startsWith(STARTING_PREFIX) && !state.startsWith(RESUMING_PREFIX)) {
            "Provisioning callback state does not match active flow"
        }
        val active = activeStates.get(ACTIVE_PROVISIONING_STATE_KEY)
        // Older versions could persist the context without an active-state marker.
        if (active != null) validateProvisioningCallbackStateValue(state, active)
        else if (contexts.get(state) == null) throw missingContext(state)

        val owner = "$RESUMING_PREFIX${UUID.randomUUID()}"
        try {
            if (!transition(expected = active, next = owner)) throw missingContext(state)
            val context = contexts.remove(state) ?: throw missingContext(state)
            return handle(context, attestations.remove(state))
        } finally {
            withContext(NonCancellable) {
                if (activeStates.get(ACTIVE_PROVISIONING_STATE_KEY) == owner) cleanUp(state, owner)
            }
        }
    }

    private suspend fun cleanUp(state: String, owner: String) {
        try {
            contexts.remove(state)
            attestations.remove(state)
            codeVerifiers.remove(state)
        } finally {
            transition(expected = owner, next = null)
        }
    }

    private suspend fun transition(expected: String?, next: String?): Boolean =
        activeStates.compareAndSet(ACTIVE_PROVISIONING_STATE_KEY, expected, next)

    private fun missingContext(state: String) =
        IllegalStateException("No provisioning context found for callback state $state")
}

internal const val ACTIVE_PROVISIONING_STATE_KEY = "active"
private const val STARTING_PREFIX = "starting:"
private const val RESUMING_PREFIX = "resuming:"
