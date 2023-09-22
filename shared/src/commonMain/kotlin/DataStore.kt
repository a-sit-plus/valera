import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.flow.first
import okio.Path.Companion.toPath

private lateinit var dataStore: DataStore<Preferences>

private val lock = SynchronizedObject()

class DataStoreService(dataStore: DataStore<Preferences>){
    private var dataStore = dataStore
    suspend fun setData(value: String, key: String){
        val dataStoreKey = stringPreferencesKey(key)

        dataStore.edit {
            it[dataStoreKey] = value
        }
    }

    suspend fun getData(key: String){
        val dataStoreKey = stringPreferencesKey(key)
        val preferences = dataStore.data.first()
        val value = preferences[dataStoreKey]

    }
}

// Modified from https://github.com/android/kotlin-multiplatform-samples/tree/main
/**
 * Gets the singleton DataStore instance, creating it if necessary.
 */
fun getDataStore(producePath: () -> String): DataStore<Preferences> =
    synchronized(lock) {
        if (::dataStore.isInitialized) {
            dataStore
        } else {
            PreferenceDataStoreFactory.createWithPath(produceFile = { producePath().toPath() })
                .also { dataStore = it }
        }
    }

internal const val dataStoreFileName = "wallet.preferences_pb"
