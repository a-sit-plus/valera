package data.storage

class DummyDataStoreService: DataStoreService {
    var memory: MutableMap<String, String> = mutableMapOf()
    override suspend fun setPreference(value: String, key: String){
        memory[key] = value
    }

    override suspend fun getPreference(key: String): String? {
        return memory[key]
    }

    override suspend fun deletePreference(key: String){
        memory.remove(key)
    }

    override fun writeLogToFile(data: ExportLog) {
    }

    override fun readLogFromFile(): MutableList<ExportLog> {
        TODO("Not yet implemented")
    }

    override fun clearLog() {
        TODO("Not yet implemented")
    }
}
