import data.storage.RealDataStoreService
import data.storage.createDataStore
import io.kotest.common.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class UnitTests {
    @Test
    fun testDataStore() {
        val dataStore = createDataStore()
        val realDataStoreService = RealDataStoreService(dataStore)
        runBlocking { realDataStoreService.setData(Resources.DEBUG_DATASTORE_VALUE, Resources.DEBUG_DATASTORE_KEY) }
        val result = runBlocking { realDataStoreService.getData(Resources.DEBUG_DATASTORE_KEY) }
        assertEquals(Resources.DEBUG_DATASTORE_VALUE, result)
    }
}