import at.asitplus.wallet.app.common.DummyPlatformAdapter
import data.storage.RealDataStoreService
import data.storage.createDataStore
import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertEquals
import at.asitplus.wallet.app.common.Configuration
import kotlinx.coroutines.runBlocking


class UnitTests {
    @Test
    fun testDataStore() {
        val dataStore = createDataStore()
        val realDataStoreService = RealDataStoreService(dataStore, DummyPlatformAdapter())
        runBlocking {
            realDataStoreService.setPreference(
                value = Configuration.DEBUG_DATASTORE_VALUE,
                key = Configuration.DEBUG_DATASTORE_KEY
            )
        }
        val result =
            runBlocking { realDataStoreService.getPreference(Configuration.DEBUG_DATASTORE_KEY).first() }
        assertEquals(Configuration.DEBUG_DATASTORE_VALUE, result)
    }
}