package at.asitplus.wallet.app.common

import at.asitplus.signum.supreme.os.PlatformSigningProvider
import at.asitplus.signum.supreme.os.SigningProvider

actual fun getProvider(): SigningProvider = PlatformSigningProvider
