package at.asitplus.wallet.app.common

import data.storage.DummyDataStoreService
import data.storage.persistentStringMapStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AbandonedProvisioningFlowTest {
    private val dataStore = DummyDataStoreService()
    private val activeStore = persistentStringMapStore(dataStore, "active")
    private val contextStore = persistentStringMapStore(dataStore, "contexts")
    private val attestationStore = persistentStringMapStore(dataStore, "attestations")

    @Test
    fun clearsAbandonedFlowSoNewProvisioningCanStart() = runTest {
        activeStore.put(ACTIVE_PROVISIONING_STATE_KEY, "abandoned-state")
        contextStore.put("abandoned-state", "context")
        attestationStore.put("abandoned-state", "attestation")

        clearAbandonedProvisioningFlow(activeStore, contextStore, attestationStore)

        assertNull(activeStore.get(ACTIVE_PROVISIONING_STATE_KEY))
        assertNull(contextStore.get("abandoned-state"))
        assertNull(attestationStore.get("abandoned-state"))
    }

    @Test
    fun keepsOtherStatesContextsWhenClearing() = runTest {
        activeStore.put(ACTIVE_PROVISIONING_STATE_KEY, "abandoned-state")
        contextStore.put("other-state", "context")

        clearAbandonedProvisioningFlow(activeStore, contextStore, attestationStore)

        assertEquals("context", contextStore.get("other-state"))
    }

    @Test
    fun doesNothingWithoutActiveFlow() = runTest {
        contextStore.put("some-state", "context")

        clearAbandonedProvisioningFlow(activeStore, contextStore, attestationStore)

        assertEquals("context", contextStore.get("some-state"))
    }
}
