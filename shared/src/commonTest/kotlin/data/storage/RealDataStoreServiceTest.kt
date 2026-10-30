package data.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import at.asitplus.wallet.app.common.DummyPlatformAdapter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertIs

class RealDataStoreServiceTest {
    private class BlockingDataStore : DataStore<Preferences> {
        override val data = MutableStateFlow(emptyPreferences())
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            awaitCancellation()
    }

    @Test
    fun cancellingAWriteIsNoFailure() = runTest {
        val service = RealDataStoreService(BlockingDataStore(), DummyPlatformAdapter())
        var thrown: Throwable? = null
        val write = launch {
            try {
                service.setPreference("value", "key")
            } catch (e: Throwable) {
                thrown = e
                throw e
            }
        }
        yield()
        write.cancel()
        write.join()
        assertIs<CancellationException>(thrown)
    }
}
