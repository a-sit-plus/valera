import data.storage.DataStoreService
import data.storage.createDataStore
import io.kotest.common.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class UnitTests {
    @Test
    fun testDataStore() {
        val dataStore = createDataStore()
        val dataStoreService = DataStoreService(dataStore)
        runBlocking { dataStoreService.setData(Resources.DEBUG_DATASTORE_VALUE, Resources.DEBUG_DATASTORE_KEY) }
        val result = runBlocking { dataStoreService.getData(Resources.DEBUG_DATASTORE_KEY) }
        assertEquals(Resources.DEBUG_DATASTORE_VALUE, result)
    }
}