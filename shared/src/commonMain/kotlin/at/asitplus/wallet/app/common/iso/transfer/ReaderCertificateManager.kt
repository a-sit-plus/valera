package at.asitplus.wallet.app.common.iso.transfer

import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcCurve
import org.multipaz.crypto.EcPrivateKey
import org.multipaz.crypto.X500Name
import org.multipaz.crypto.X509Cert
import org.multipaz.mdoc.util.MdocUtil
import org.multipaz.asn1.ASN1Integer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.multipaz.crypto.EcPublicKey

// based on identity-credential[https://github.com/openwallet-foundation-labs/identity-credential] implementation
/**
 * Manages the keys and certificates used by the mdoc reader.
 */
class ReaderCertificateManager {
    val certsValidFrom = LocalDate.parse("2024-12-01").atStartOfDayIn(TimeZone.UTC)
    val certsValidUntil = LocalDate.parse("2034-12-01").atStartOfDayIn(TimeZone.UTC)

    val readerRootKey: EcPrivateKey by lazy { bundledReaderRootKey }
    val readerRootCert: X509Cert by lazy { bundledReaderRootCert }
    val readerKey: EcPrivateKey by lazy { Crypto.createEcPrivateKey(EcCurve.P256) }
    val readerCert: X509Cert by lazy { createReaderCert() }

    private val bundledReaderRootKey: EcPrivateKey by lazy {
        val readerRootKeyPub = EcPublicKey.fromPem(
            """
                    -----BEGIN PUBLIC KEY-----
                    MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAE+QDye70m2O0llPXMjVjxVZz3m5k6agT+
                    wih+L79b7jyqUl99sbeUnpxaLD+cmB3HK3twkA7fmVJSobBc+9CDhkh3mx6n+YoH
                    5RulaSWThWBfMyRjsfVODkosHLCDnbPV
                    -----END PUBLIC KEY-----
                """.trimIndent().trim(),
            EcCurve.P384
        )
        EcPrivateKey.fromPem(
            """
                    -----BEGIN PRIVATE KEY-----
                    MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDCcRuzXW3pW2h9W8pu5
                    /CSR6JSnfnZVATq+408WPoNC3LzXqJEQSMzPsI9U1q+wZ2yhZANiAAT5APJ7vSbY
                    7SWU9cyNWPFVnPebmTpqBP7CKH4vv1vuPKpSX32xt5SenFosP5yYHccre3CQDt+Z
                    UlKhsFz70IOGSHebHqf5igflG6VpJZOFYF8zJGOx9U4OSiwcsIOds9U=
                    -----END PRIVATE KEY-----
                """.trimIndent().trim(),
            readerRootKeyPub
        )
    }

    private val bundledReaderRootCert: X509Cert by lazy {
        MdocUtil.generateReaderRootCertificate(
            readerRootKey = readerRootKey,
            subject = X500Name.fromName("CN=OWF IC TestApp Reader Root"),
            serial = ASN1Integer(1L),
            validFrom = certsValidFrom,
            validUntil = certsValidUntil
        )
    }

    private fun createReaderCert(): X509Cert {
        return MdocUtil.generateReaderCertificate(
            readerRootCert = readerRootCert,
            readerRootKey = readerRootKey,
            readerKey = readerKey.publicKey,
            subject = X500Name.fromName("CN=OWF IC TestApp Reader Cert"),
            serial = ASN1Integer(1L),
            validFrom = certsValidFrom,
            validUntil = certsValidUntil
        )
    }
}
