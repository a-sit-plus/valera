package data.storage

class DummyDataStoreService: DataStoreService{
    lateinit var memory: MutableMap<String, String>
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
