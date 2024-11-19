
import at.asitplus.wallet.app.common.PlatformAdapter

class AndroidDummyPlatformAdapter() : PlatformAdapter {
    override fun openUrl(url: String) {

    }
    
    override fun writeToFile(text: String, fileName: String, folderName: String) {
    }

    override fun readFromFile(fileName: String, folderName: String): String? {
        return null
    }

    override fun clearFile(fileName: String, folderName: String) {
    }

    override fun shareLog() {
    }
}