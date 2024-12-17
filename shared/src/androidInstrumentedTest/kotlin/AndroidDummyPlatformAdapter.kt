
import at.asitplus.wallet.app.common.PlatformAdapter
import data.dcapi.DCAPIRequest
import data.dcapi.CredentialsContainer

class AndroidDummyPlatformAdapter : PlatformAdapter {
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

    override fun registerWithDigitalCredentialsAPI(entries: CredentialsContainer) {
    }

    override fun getCurrentDCAPIData(): DCAPIRequest? {
        return null
    }

    override fun sendAPIResultBack(responseJson: ByteArray, dcApiRequest: DCAPIRequest) {
    }
}