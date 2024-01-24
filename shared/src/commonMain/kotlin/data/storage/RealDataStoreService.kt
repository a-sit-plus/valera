package data.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import at.asitplus.wallet.app.common.PlatformAdapter
import at.asitplus.wallet.lib.data.jsonSerializer
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import okio.Path.Companion.toPath

interface DataStoreService {
    suspend fun setPreference(value: String, key: String)
    suspend fun getPreference(key: String): String?
    suspend fun deletePreference(key: String)
    fun writeLogToFile(data: exportLog)
    fun readLogFromFile(): MutableList<exportLog>
    fun clearLog()

}
class RealDataStoreService(private var dataStore: DataStore<Preferences>, private var platformAdapter: PlatformAdapter): DataStoreService{
    override suspend fun setPreference(value: String, key: String){
        try {
            val dataStoreKey = stringPreferencesKey(key)
            dataStore.edit {
                it[dataStoreKey] = value
            }
        } catch (e: Throwable) {
            throw Exception("Unable to set data in DataStore")
        }

    }

    override suspend fun getPreference(key: String): String? {
        try {
            val dataStoreKey = stringPreferencesKey(key)
            val preferences = dataStore.data.first()
            return preferences[dataStoreKey]
        } catch (e: Throwable) {
            throw Exception("Unable to get data from DataStore")
        }

    }

    override suspend fun deletePreference(key: String){
        try {
            val dataStoreKey = stringPreferencesKey(key)
            dataStore.edit {
                it.remove(dataStoreKey)
            }
        } catch (e: Throwable) {
            throw Exception("Unable to delete data from DataStore")
        }

    }

    override fun writeLogToFile(data: exportLog) {
        val json = jsonSerializer.encodeToString(data)
        platformAdapter.writeToFile(text = "$json\n\n", fileName = "log.txt")
    }

    override fun readLogFromFile(): MutableList<exportLog> {
        val raw = this.platformAdapter.readFromFile(fileName = "log.txt") ?: ""
        val rawArray = raw.split("\n\n")
        val array = mutableListOf<exportLog>()
        rawArray.filter{ it.length > 0 }.forEach {
            println("it: ${it.length}")
            val item = jsonSerializer.decodeFromString<exportLog>(it)
            array.add(item)
        }
        return array
    }

    override fun clearLog() {
        platformAdapter.clearFile(fileName = "log.txt")
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

private lateinit var dataStore: DataStore<Preferences>

private val lock = SynchronizedObject()