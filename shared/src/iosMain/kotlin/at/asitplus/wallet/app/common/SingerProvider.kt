package at.asitplus.wallet.app.common


import at.asitplus.signum.supreme.os.IosKeychainProvider
import at.asitplus.signum.supreme.os.SigningProvider

actual fun getProvider(): SigningProvider = IosKeychainProvider