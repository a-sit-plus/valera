import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.KmmResult
import at.asitplus.wallet.app.common.HolderKeyService
import at.asitplus.wallet.app.common.ObjectFactory
import at.asitplus.wallet.app.common.PlatformAdapter
import at.asitplus.wallet.lib.agent.CryptoService

actual fun getObjectFactory(): ObjectFactory {
    return DummySwiftObjectFactory()
}

@Composable
actual fun getPlatformAdapter(): PlatformAdapter {
    return DummySwiftPlatformAdapter()
}

class DummySwiftObjectFactory : ObjectFactory {
    private val keyChainService: RealKeyChainService by lazy { RealKeyChainService() }

    override fun loadCryptoService(): KmmResult<CryptoService> {
        return try {
            keyChainService.initialize()
            val cryptoService = VcLibCryptoServiceCryptoKit(keyChainService = keyChainService)
                ?: return KmmResult.failure(Throwable("Error on creating VcLibCryptoServiceCryptoKit"))
            KmmResult.success(cryptoService)
        } catch (e: Exception) {
            KmmResult.failure(Throwable("Error from keyChainService.generateKeyPair"))
        }
    }

    override fun loadHolderKeyService(): KmmResult<HolderKeyService> {
        return KmmResult.success(keyChainService)
    }
}

class DummySwiftPlatformAdapter() : PlatformAdapter {
    override fun openUrl(url: String) {

    }

    override fun decodeImage(image: ByteArray): ImageBitmap {
        return IosUtilities.decodeImage(image)
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