package at.asitplus.wallet.app.common

import at.asitplus.signum.supreme.os.AndroidKeyStoreProvider
import at.asitplus.signum.supreme.os.SigningProvider


actual fun getProvider(): SigningProvider = AndroidKeyStoreProvider()