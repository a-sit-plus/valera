package data.storage

class DummyDataStoreService: DataStoreService {
    var memory: MutableMap<String, String> = mutableMapOf()
    override suspend fun setData(value: String, key: String){
        memory = mutableMapOf(key to value)
    }

    override suspend fun getData(key: String): String? {
        return memory[key]
    }

    override suspend fun deleteData(key: String){
        memory.remove(key)
    }
}
