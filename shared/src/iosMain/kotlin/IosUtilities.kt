import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image


object IosUtilities{
    fun decodeImage(image: ByteArray): ImageBitmap {
        return Image.makeFromEncoded(image).toComposeImageBitmap()
    }
}
