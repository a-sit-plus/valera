import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import at.asitplus.KmmResult
import at.asitplus.wallet.app.common.HolderKeyService
import at.asitplus.wallet.app.common.ObjectFactory
import at.asitplus.wallet.app.common.PlatformAdapter
import at.asitplus.wallet.lib.agent.CryptoService
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import java.awt.Desktop
import java.net.URI


class JvmObjectFactory : ObjectFactory {
    init {
        Napier.takeLogarithm()
        Napier.base(DebugAntilog())
    }

    override fun loadCryptoService(): KmmResult<CryptoService> {
        return KmmResult.failure(Throwable("Incomplete Implementation"))
    }

    override fun loadHolderKeyService(): KmmResult<HolderKeyService> {
        return KmmResult.failure(Throwable("Incomplete Implementation"))
    }
}

class JvmPlatformAdapter(): PlatformAdapter {
    override fun openUrl(url: String) {
        // source: https://stackoverflow.com/questions/68306576/open-a-link-in-browser-using-compose-for-desktop
        Desktop.getDesktop().browse(URI(url))
    }

    override fun decodeImage(image: ByteArray): ImageBitmap {
        throw Exception("Incomplete Implementation")
    }
}