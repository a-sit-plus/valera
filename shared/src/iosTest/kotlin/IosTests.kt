
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.kotest.common.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
class IosTests {
    @Test
    fun testDataStore() {
        val dataStore = createDataStore()
        val dataStoreService = DataStoreService(dataStore)
        runBlocking { dataStoreService.setData(Resources.DEBUG_DATASTORE_VALUE, Resources.DEBUG_DATASTORE_KEY) }
        val result = runBlocking { dataStoreService.getData(Resources.DEBUG_DATASTORE_KEY) }
        assertEquals(Resources.DEBUG_DATASTORE_VALUE, result)
    }
}