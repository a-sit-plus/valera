package at.asitplus.wallet.app
import at.asitplus.wallet.app.common.Configuration
import data.storage.DummyDataStoreService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class CommonUnitTests {
    @Test
    fun testDataStoreService() {
        val dataStoreService = DummyDataStoreService()
        runBlocking { dataStoreService.setPreference(value = Configuration.DEBUG_DATASTORE_VALUE, key = Configuration.DEBUG_DATASTORE_KEY) }
        val result = runBlocking { dataStoreService.getPreference(key = Configuration.DEBUG_DATASTORE_KEY).first() }
        assertEquals(Configuration.DEBUG_DATASTORE_VALUE, result)
    }

    @Test
    fun testDataStoreServiceFlow() {
        val dataStoreService = DummyDataStoreService()

        val delegate = dataStoreService.getPreference(Configuration.DEBUG_DATASTORE_KEY)

        runBlocking {
            assertEquals(null, delegate.firstOrNull())
        }
        runBlocking {
            dataStoreService.setPreference(value = "0", key = Configuration.DEBUG_DATASTORE_KEY)
        }
        runBlocking {
            assertEquals("0", delegate.firstOrNull())
        }
        runBlocking {
            dataStoreService.setPreference(value = "1", key = Configuration.DEBUG_DATASTORE_KEY)
        }
        runBlocking {
            assertEquals("1", delegate.firstOrNull())
        }
    }
}