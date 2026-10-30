package data.storage

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import at.asitplus.wallet.app.common.Configuration
import at.asitplus.wallet.app.common.PlatformAdapter
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath
import io.github.aakira.napier.Napier
import kotlin.time.measureTime

interface DataStoreService {
    suspend fun setPreference(value: String, key: String)
    fun getPreference(key: String): Flow<String?>
    suspend fun deletePreference(key: String)
    suspend fun deletePreferenceIfPresent(key: String): Boolean
    fun clearLog()
}

/**
 * Stores caches, see [isCacheKey], in [cacheDataStore] and everything else in [dataStore]: DataStore rewrites its whole
 * file for every change, one change after the other, so a small write, e.g. of a provisioning state, waited for
 * several writes of megabytes of cached HTTP responses and Trust Lists.
 */
class RealDataStoreService(
    private val dataStore: DataStore<Preferences>,
    private val platformAdapter: PlatformAdapter,
    private val cacheDataStore: DataStore<Preferences>,
) : DataStoreService {
    private fun storeOf(key: String) = if (isCacheKey(key)) cacheDataStore else dataStore

    override suspend fun setPreference(value: String, key: String){
        try {
            val dataStoreKey = stringPreferencesKey(key)
            measureTime {
                storeOf(key).edit {
                    it[dataStoreKey] = value
                }
            }.also { Napier.d("Timing: wrote $key (${value.length} characters) to DataStore in $it") }
        } catch (e: Throwable) {
            throw Exception("Unable to set data in DataStore")
        }

    }

    override fun getPreference(key: String): Flow<String?> {
        try {
            val dataStoreKey = stringPreferencesKey(key)
            val preferences = storeOf(key).data
            return preferences.map { it[dataStoreKey] }
        } catch (e: Throwable) {
            throw Exception("Unable to get data from DataStore")
        }

    }

    override suspend fun deletePreference(key: String){
        try {
            val dataStoreKey = stringPreferencesKey(key)
            storeOf(key).edit {
                it.remove(dataStoreKey)
            }
        } catch (e: Throwable) {
            throw Exception("Unable to delete data from DataStore")
        }

    }

    override suspend fun deletePreferenceIfPresent(key: String): Boolean {
        val dataStoreKey = stringPreferencesKey(key)
        var removed = false
        storeOf(key).edit { preferences ->
            removed = preferences.remove(dataStoreKey) != null
        }
        return removed
    }

    override fun clearLog() {
        platformAdapter.clearFile(fileName = "log.txt", folderName = "logs")
    }
}

/** Whether [key] holds a cache, which is stored in its own DataStore file, see [RealDataStoreService]. */
internal fun isCacheKey(key: String) =
    key in cacheKeys ||
        // The HTTP cache stores the responses of each URL, and an index of them, under keys of its own
        key.startsWith(Configuration.DATASTORE_KEY_HTTP_CACHE) ||
        key.startsWith("https://") || key.startsWith("http://") // Trust Lists are stored by URL

private val cacheKeys = setOf(
    Configuration.DATASTORE_KEY_TOKEN_STATUS_LIST_CACHE,
    Configuration.DATASTORE_KEY_CREDENTIAL_METADATA_CACHE,
)

/**
 * Drops the caches from the main DataStore, which held them before they moved to their own file. They are not moved,
 * but loaded again when needed.
 */
internal object DropCachesFromMainDataStore : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences) =
        currentData.asMap().keys.any { isCacheKey(it.name) }

    override suspend fun migrate(currentData: Preferences): Preferences =
        currentData.toMutablePreferences().apply {
            currentData.asMap().keys.filter { isCacheKey(it.name) }.forEach { remove(it) }
        }

    override suspend fun cleanUp() {}
}

// Modified from https://github.com/android/kotlin-multiplatform-samples/tree/main
/**
 * Gets the singleton DataStore instance, creating it if necessary.
 */
fun getDataStore(producePath: () -> String): DataStore<Preferences> =
    getDataStoreWithFactory {
        PreferenceDataStoreFactory.createWithPath(
            migrations = listOf(DropCachesFromMainDataStore),
            produceFile = { producePath().toPath() },
        )
    }

/** Gets the singleton DataStore instance of the caches, see [RealDataStoreService], creating it if necessary. */
fun getCacheDataStore(producePath: () -> String): DataStore<Preferences> =
    getCacheDataStoreWithFactory {
        PreferenceDataStoreFactory.createWithPath(produceFile = { producePath().toPath() })
    }

fun getDataStoreWithFactory(createDataStore: () -> DataStore<Preferences>): DataStore<Preferences> =
    synchronized(lock) {
        if (::dataStore.isInitialized) {
            dataStore
        } else {
            createDataStore().also { dataStore = it }
        }
    }

fun getCacheDataStoreWithFactory(createDataStore: () -> DataStore<Preferences>): DataStore<Preferences> =
    synchronized(lock) {
        if (::cacheDataStore.isInitialized) {
            cacheDataStore
        } else {
            createDataStore().also { cacheDataStore = it }
        }
    }

internal const val dataStoreFileName = "wallet.preferences_pb"
internal const val cacheDataStoreFileName = "wallet-cache.preferences_pb"

private lateinit var dataStore: DataStore<Preferences>
private lateinit var cacheDataStore: DataStore<Preferences>

private val lock = SynchronizedObject()
