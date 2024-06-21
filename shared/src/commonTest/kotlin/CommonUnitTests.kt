
import at.asitplus.wallet.app.common.Configuration
import data.storage.DummyDataStoreService
import kotlinx.coroutines.flow.first
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
}