import at.asitplus.KmmResult
import at.asitplus.crypto.datatypes.pki.X509Certificate
import at.asitplus.wallet.app.android.AndroidCryptoService
import at.asitplus.wallet.app.android.AndroidKeyStoreService
import at.asitplus.wallet.app.common.HolderKeyService
import at.asitplus.wallet.app.common.ObjectFactory
import at.asitplus.wallet.app.common.PlatformAdapter
import at.asitplus.wallet.lib.agent.CryptoService

class AndroidObjectFactory : ObjectFactory {
    val keyStoreService: AndroidKeyStoreService by lazy { AndroidKeyStoreService() }

    override fun loadCryptoService(): KmmResult<CryptoService> {
        val keyPair = keyStoreService.loadKeyPair()
            ?: return KmmResult.failure(Throwable("Could not create key pair"))
        val certificate =
            keyStoreService.loadCertificate()?.let { X509Certificate.decodeFromDer(it.encoded) }
                ?: return KmmResult.failure(Throwable("Could not load certificate"))
        val cryptoService = AndroidCryptoService(keyPair, certificate)
        return KmmResult.success(cryptoService)
    }

    override fun loadHolderKeyService(): KmmResult<HolderKeyService> {
        return KmmResult.success(keyStoreService)
    }
}

actual fun getPlatformAdapter(): PlatformAdapter {
    return AndroidDummyPlatformAdapter()
}

actual fun getObjectFactory(): ObjectFactory {
    return AndroidObjectFactory()
}