package data.storage

import at.asitplus.wallet.app.common.Configuration
import at.asitplus.wallet.app.common.ErrorService
import io.ktor.http.Cookie
import io.ktor.http.Url
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PersistentCookieStorageTest {
    private val url = Url("https://issuer.example.test/path")

    @Test
    fun addsCookieWithoutWaitingForTheWrite() = runTest {
        val writeStarted = CompletableDeferred<Unit>()
        val releaseWrite = CompletableDeferred<Unit>()
        val delegate = DummyDataStoreService()
        val dataStore = object : DataStoreService by delegate {
            override suspend fun setPreference(value: String, key: String) {
                writeStarted.complete(Unit)
                releaseWrite.await()
                delegate.setPreference(value, key)
            }
        }
        val storage = PersistentCookieStorage(dataStore, ErrorService(backgroundScope), coroutineContext)

        storage.addCookie(url, Cookie(name = "SESSION", value = "first"))
        writeStarted.await()
        // The write is still blocked, but the cookie is already in use
        assertEquals(listOf("first"), storage.get(url).map { it.value })
        assertNull(dataStore.getPreference(Configuration.DATASTORE_KEY_COOKIES).first())

        releaseWrite.complete(Unit)
        storage.closeAndAwaitWrites()
        assertTrue(dataStore.getPreference(Configuration.DATASTORE_KEY_COOKIES).first()!!.contains("first"))
    }

    @Test
    fun persistsTheLatestCookies() = runTest {
        val dataStore = DummyDataStoreService()
        val writer = StandardTestDispatcher(testScheduler)
        val storage = PersistentCookieStorage(dataStore, ErrorService(backgroundScope), writer)

        storage.addCookie(url, Cookie(name = "SESSION", value = "first"))
        storage.addCookie(url, Cookie(name = "SESSION", value = "second"))
        storage.closeAndAwaitWrites()

        // A new instance, i.e. after a restart, reads what has been written last
        val restored = PersistentCookieStorage(dataStore, ErrorService(backgroundScope), writer)
        assertEquals(listOf("second"), restored.get(url).map { it.value })
        restored.closeAndAwaitWrites()
    }

    @Test
    fun resetRemovesPersistedCookies() = runTest {
        val dataStore = DummyDataStoreService()
        val storage = PersistentCookieStorage(dataStore, ErrorService(backgroundScope), coroutineContext)
        storage.addCookie(url, Cookie(name = "SESSION", value = "first"))
        storage.reset()
        storage.closeAndAwaitWrites()

        val restored = PersistentCookieStorage(dataStore, ErrorService(backgroundScope), coroutineContext)
        assertTrue(restored.get(url).isEmpty())
        restored.closeAndAwaitWrites()
    }
}
