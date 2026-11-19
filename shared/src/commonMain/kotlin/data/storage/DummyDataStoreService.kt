package data.storage

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class DummyDataStoreService: DataStoreService {
    private val lock = SynchronizedObject()
    private val memory = mutableMapOf<String, MutableStateFlow<String?>>()

    private fun preference(key: String): MutableStateFlow<String?> = synchronized(lock) {
        memory.getOrPut(key) { MutableStateFlow(null) }
    }

    override suspend fun setPreference(value: String, key: String) {
        preference(key).value = value
    }

    override fun getPreference(key: String): Flow<String?> = preference(key)

    override suspend fun deletePreference(key: String) {
        preference(key).value = null
    }

    override suspend fun deletePreferenceIfPresent(key: String): Boolean {
        val preference = preference(key)
        while (true) {
            val value = preference.value ?: return false
            if (preference.compareAndSet(value, null)) return true
        }
    }

    override suspend fun updatePreference(key: String, transform: (String?) -> String?) {
        preference(key).update(transform)
    }

    override fun clearLog() {
    }
}
