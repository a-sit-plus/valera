package data.storage

class DummyDataStoreService(){
    lateinit var memory: MutableMap<String, String>
    suspend fun setData(value: String, key: String){
        memory = mutableMapOf(key to value)
    }

    suspend fun getData(key: String): String? {
        return memory[key]
    }

    suspend fun deleteData(key: String){
        memory.remove(key)
    }
}
