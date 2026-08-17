package at.asitplus.wallet.app.common

import java.net.SocketException
import java.net.UnknownHostException
import java.security.NoSuchAlgorithmException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TlsErrorTest {
    @Test
    fun recognizesWrappedTlsSetupFailure() {
        val failure = SocketException().apply { initCause(NoSuchAlgorithmException()) }

        assertTrue(failure.isTlsError())
        assertFalse(UnknownHostException().isTlsError())
    }
}
