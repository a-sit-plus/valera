package at.asitplus.wallet.app.common

import java.security.GeneralSecurityException
import javax.net.ssl.SSLException

internal actual fun Throwable.isPlatformTlsError(): Boolean =
    this is SSLException || this is GeneralSecurityException
