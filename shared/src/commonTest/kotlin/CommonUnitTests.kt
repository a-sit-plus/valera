
import data.storage.DummyDataStoreService
import io.kotest.common.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class CommonUnitTests {
    @Test
    fun testDataStoreService() {
        val dataStoreService = DummyDataStoreService()
        runBlocking { dataStoreService.setPreference(value = Resources.DEBUG_DATASTORE_VALUE, key = Resources.DEBUG_DATASTORE_KEY) }
        val result = runBlocking { dataStoreService.getPreference(key = Resources.DEBUG_DATASTORE_KEY) }
        assertEquals(Resources.DEBUG_DATASTORE_VALUE, result)
    }
}