package at.asitplus.wallet.app.common.trust

import at.asitplus.signum.indispensable.pki.X509Certificate
import at.asitplus.wallet.app.common.asitRootPem
import kotlin.test.Test
import kotlin.test.assertEquals

class RegistrationCertificateAnchorsTest {
    private val root = X509Certificate.decodeFromPem(asitRootPem).getOrThrow()

    @Test
    fun bundleWithoutCertificatesIsEmpty() {
        assertEquals(emptySet(), parsePemCertificates("Root certificates of WRPRC providers\n"))
    }

    @Test
    fun certificatesAreReadBetweenOtherText() {
        val bundle = "Test anchors\n$asitRootPem\nthe same again\n$asitRootPem"

        assertEquals(setOf(root), parsePemCertificates(bundle))
    }
}
