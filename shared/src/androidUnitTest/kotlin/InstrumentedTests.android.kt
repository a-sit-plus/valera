import at.asitplus.wallet.app.common.ObjectFactory
import at.asitplus.wallet.app.common.PlatformAdapter

actual fun getObjectFactory(): ObjectFactory {
    return AndroidObjectFactory()
}

actual fun getPlatformAdapter(): PlatformAdapter {
    TODO("Not yet implemented")
}