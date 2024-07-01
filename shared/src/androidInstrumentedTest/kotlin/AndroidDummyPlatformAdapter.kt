import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import at.asitplus.wallet.app.common.PlatformAdapter

class AndroidDummyPlatformAdapter() : PlatformAdapter {
    override fun openUrl(url: String) {

    }

    override fun decodeImage(image: ByteArray): ImageBitmap {
        val bitmap = BitmapFactory.decodeByteArray(image, 0, image.size)
        bitmap.describeContents()
        return bitmap.asImageBitmap()
    }

    override fun writeToFile(text: String, fileName: String, folderName: String) {
    }

    override fun readFromFile(fileName: String, folderName: String): String? {
        return null
    }

    override fun clearFile(fileName: String, folderName: String) {
    }

    override fun exitApp() {
    }

    override fun shareLog() {
    }
}