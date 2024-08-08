
import at.asitplus.wallet.app.common.PlatformAdapter
import java.util.Base64

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

    override fun imageStringToBytearray(imageString: String): ByteArray {
        return Base64.getDecoder().decode(imageString)
    }
}