package data.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class DummyDataStoreService: DataStoreService {
    private var memory: MutableMap<String, MutableStateFlow<String?>> = mutableMapOf()

    override suspend fun setData(value: String, key: String) {
        memory.get(key)?.update { value }
    }

    override fun getData(key: String): Flow<String?> {
        return memory.getOrPut(key) {
            MutableStateFlow(null)
        }
    }

    override suspend fun deleteData(key: String) {
        memory.get(key)?.update { null }
    }
}
