package data.storage

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeferredRemovalMapStoreTest {
    private var writes = 0
    private val delegate = DummyDataStoreService()
    private val dataStore = object : DataStoreService by delegate {
        override suspend fun setPreference(value: String, key: String) {
            writes++
            delegate.setPreference(value, key)
        }

        override suspend fun deletePreference(key: String) {
            writes++
            delegate.deletePreference(key)
        }
    }
    private val persistent = persistentStringMapStore(dataStore, "codes")

    @Test
    fun removeTakesTheValueWithoutWriting() = runTest {
        persistent.put("state", "verifier")
        persistent.put("other", "kept")
        val store = DeferredRemovalMapStore(persistent)
        writes = 0

        assertEquals("verifier", store.remove("state"))
        assertEquals(0, writes)
        assertNull(store.get("state"))
        assertNull(store.remove("state"))

        store.flushRemovals()
        assertEquals(1, writes)
        assertNull(persistent.get("state"))
        assertEquals("kept", persistent.get("other"))
    }

    @Test
    fun putAfterRemoveIsVisible() = runTest {
        val store = DeferredRemovalMapStore(persistent)
        store.put("state", "first")
        store.remove("state")
        store.put("state", "second")

        assertEquals("second", store.get("state"))
        store.flushRemovals()
        assertEquals("second", persistent.get("state"))
    }

    @Test
    fun persistentRemoveOfMissingKeyDoesNotWrite() = runTest {
        persistent.put("state", "verifier")
        writes = 0

        assertNull(persistent.remove("missing"))
        assertEquals(0, writes)
    }
}
