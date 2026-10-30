package data.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import at.asitplus.wallet.app.common.Configuration
import at.asitplus.wallet.app.common.DummyPlatformAdapter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CacheDataStoreTest {
    private class InMemoryDataStore : DataStore<Preferences> {
        override val data = MutableStateFlow(emptyPreferences())
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences) =
            transform(data.value).also { data.value = it }
    }

    private val trustListUrl = "https://trust.example.test/lists/pid-providers.json"

    @Test
    fun storesCachesInTheirOwnDataStore() = runTest {
        val main = InMemoryDataStore()
        val cache = InMemoryDataStore()
        val service = RealDataStoreService(main, DummyPlatformAdapter(), cache)

        val httpCacheEntries = "${Configuration.DATASTORE_KEY_HTTP_CACHE}|$trustListUrl"
        val httpCacheIndex = "${Configuration.DATASTORE_KEY_HTTP_CACHE}_index"
        service.setPreference("cached", Configuration.DATASTORE_KEY_HTTP_CACHE)
        service.setPreference("entries", httpCacheEntries)
        service.setPreference("index", httpCacheIndex)
        service.setPreference("list", trustListUrl)
        service.setPreference("credentials", Configuration.DATASTORE_KEY_VCS)

        assertEquals(
            setOf(Configuration.DATASTORE_KEY_HTTP_CACHE, httpCacheEntries, httpCacheIndex, trustListUrl),
            cache.data.value.asMap().keys.map { it.name }.toSet(),
        )
        assertEquals(setOf(Configuration.DATASTORE_KEY_VCS), main.data.value.asMap().keys.map { it.name }.toSet())
        assertEquals("list", service.getPreference(trustListUrl).first())

        assertTrue(service.deletePreferenceIfPresent(trustListUrl))
        service.deletePreference(Configuration.DATASTORE_KEY_HTTP_CACHE)
        service.deletePreference(httpCacheEntries)
        service.deletePreference(httpCacheIndex)
        assertTrue(cache.data.value.asMap().isEmpty())
        assertEquals("credentials", service.getPreference(Configuration.DATASTORE_KEY_VCS).first())
    }

    @Test
    fun migrationDropsOnlyCachesFromTheMainDataStore() = runTest {
        val stored = mutablePreferencesOf(
            stringPreferencesKey(Configuration.DATASTORE_KEY_VCS) to "credentials",
            stringPreferencesKey(Configuration.DATASTORE_KEY_COOKIES) to "cookies",
            stringPreferencesKey(Configuration.DATASTORE_KEY_HTTP_CACHE) to "cached",
            stringPreferencesKey("${Configuration.DATASTORE_KEY_HTTP_CACHE}|$trustListUrl") to "entries",
            stringPreferencesKey(Configuration.DATASTORE_KEY_TOKEN_STATUS_LIST_CACHE) to "tokens",
            stringPreferencesKey(Configuration.DATASTORE_KEY_CREDENTIAL_METADATA_CACHE) to "metadata",
            stringPreferencesKey(trustListUrl) to "list",
        )
        assertTrue(DropCachesFromMainDataStore.shouldMigrate(stored))

        val migrated = DropCachesFromMainDataStore.migrate(stored)

        assertEquals(
            setOf(Configuration.DATASTORE_KEY_VCS, Configuration.DATASTORE_KEY_COOKIES),
            migrated.asMap().keys.map { it.name }.toSet(),
        )
        assertFalse(DropCachesFromMainDataStore.shouldMigrate(migrated))
        assertNull(migrated[stringPreferencesKey(trustListUrl)])
    }
}
