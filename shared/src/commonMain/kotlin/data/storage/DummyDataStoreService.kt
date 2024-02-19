package data.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class DummyDataStoreService: DataStoreService {
    private var memory: MutableMap<String, MutableStateFlow<String?>> = mutableMapOf()

    override suspend fun setPreference(value: String, key: String) {
        memory.get(key)?.update { value }
    }

    override fun getPreference(key: String): Flow<String?> {
        return memory.getOrPut(key) {
            MutableStateFlow(null)
        }
    }

    override suspend fun deletePreference(key: String) {
        memory.get(key)?.update { null }
    }

    override fun clearLog() {
    }
}
