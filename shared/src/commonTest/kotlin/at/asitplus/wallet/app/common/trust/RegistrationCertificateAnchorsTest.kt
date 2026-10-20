package at.asitplus.wallet.app.common.trust

import at.asitplus.signum.indispensable.pki.X509Certificate
import at.asitplus.wallet.app.common.asitRootPem
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RegistrationCertificateAnchorsTest {
    private val root = X509Certificate.decodeFromPem(asitRootPem).getOrThrow()
    private val signer = X509Certificate.decodeFromDer(Base64.decode(SIGNER_DER))

    @Test
    fun bundleWithoutCertificatesIsEmpty() {
        assertEquals(emptySet(), parsePemCertificates("Root certificates of WRPRC providers\n"))
    }

    @Test
    fun certificatesAreReadBetweenOtherText() {
        val bundle = "Test anchors\n$asitRootPem\nthe same again\n$asitRootPem"

        assertEquals(setOf(root), parsePemCertificates(bundle))
    }

    @Test
    fun derCertificateIsRead() {
        assertEquals(setOf(signer), parseCertificates(Base64.decode(SIGNER_DER)))
    }

    @Test
    fun pemBundleContainsProvidersOnly() {
        val anchors = parseRegistrationTrustAnchors("Test anchors\n$asitRootPem".encodeToByteArray())

        assertEquals(RegistrationTrustAnchors(providers = setOf(root), statusListSigners = emptySet()), anchors)
    }

    @Test
    fun zipArchiveIsSplitByFolder() {
        // Windows paths, a deflated PEM provider, a stored DER signer, a file in another folder, and one without
        // a certificate, like the archive of the EUDI Wallets Launchpad 2026.
        val anchors = parseRegistrationTrustAnchors(Base64.decode(LAUNCHPAD_LIKE_ZIP))

        assertEquals(RegistrationTrustAnchors(providers = setOf(root), statusListSigners = setOf(signer)), anchors)
    }

    @Test
    fun zipEntriesAreInflated() {
        val files = ZipArchive.files(Base64.decode(LAUNCHPAD_LIKE_ZIP))

        assertEquals(
            setOf(
                "WRPRC Provider CA Certificates/A-SIT Root.cer",
                "WRPRC Revocation List Signing Certificates/ Status Signer.cer",
                "Other Certificates/Signer.cer",
                "WRPRC Provider CA Certificates/readme.txt",
            ),
            files.keys,
        )
        assertEquals("no certificate here", files.getValue("WRPRC Provider CA Certificates/readme.txt").decodeToString())
    }

    @Test
    fun truncatedZipIsRejected() {
        val zip = Base64.decode(LAUNCHPAD_LIKE_ZIP)

        assertFailsWith<IllegalArgumentException> { parseRegistrationTrustAnchors(zip.copyOf(zip.size / 2)) }
    }

    private companion object {
        // Self-signed test certificate "CN=Test WRPRC Status List Signer", no real trust anchor.
        const val SIGNER_DER =
            "MIIBpDCCAUugAwIBAgIUWnxFvSOLT8aVs3dcgdr51wcgJbswCgYIKoZIzj0EAwIwKDEmMCQGA1UEAwwdVGVzdCBXUlBSQyBTdGF0" +
                    "dXMgTGlzdCBTaWduZXIwHhcNMjYxMDE5MDkzNjAwWhcNMzYxMDE2MDkzNjAwWjAoMSYwJAYDVQQDDB1UZXN0IFdSUFJDIFN0" +
                    "YXR1cyBMaXN0IFNpZ25lcjBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABM2x5+AUpzTEpLKaPQMdo9ewYI7CwM1nXFqBDslt" +
                    "ET9+vEwnPH6p4kx5W3kBCoiXHRwpsBs6IKp7cnih2OcCWhSjUzBRMB0GA1UdDgQWBBRpuzc8HOXUVnza+TRCXyi/vVvKrjAf" +
                    "BgNVHSMEGDAWgBRpuzc8HOXUVnza+TRCXyi/vVvKrjAPBgNVHRMBAf8EBTADAQH/MAoGCCqGSM49BAMCA0cAMEQCIHoEGpCu" +
                    "po74+eH9vztLZHgWzbIjKj02rGdhtmJASCt3AiB28pvrYfJlSq1EtberPFqdeuDYdapaqFcFE3fAlUkQRQ=="

        /**
         * `WRPRC Provider CA Certificates\A-SIT Root.cer` ([asitRootPem], deflated),
         * `WRPRC Revocation List Signing Certificates\ Status Signer.cer` ([SIGNER_DER], stored),
         * `Other Certificates\Signer.cer` ([SIGNER_DER] as PEM), and `WRPRC Provider CA Certificates\readme.txt`.
         */
        const val LAUNCHPAD_LIKE_ZIP =
            "UEsDBBQAAAAIAKBLU138zvEdUwIAAD0DAAAtAAAAV1JQUkMgUHJvdmlkZXIgQ0EgQ2VydGlmaWNhdGVzXEEtU0lUIFJvb3QuY2Vy" +
                    "ZVPBkqIwEL3zFXufmhJUGD3MoRMiREicCKh4E9CwEUVlnKBfv6JVW7W1ubzK67zu1+nq9/fHQcSj/Bcm85hOKIaYdOS7wSjF/I4x" +
                    "FI4ETRFImiyCrLFre2upIw3F1yW4NOi4zqbKJhrLlAb1mt6VSR7vtcFiEjLYe2AlBJUMi4VoXQUMSb5AUDOUkO/ErBI2p5pA6i6E" +
                    "cF2opiLhiLpdApVqv8x5h9wlfRYLi7vCWj65/F9OQctEo7F4JvKIniJj4ZIZA/10gKWeiMiKo4cLokC8XDCGkwoJk0im4MYVWjPE" +
                    "PHQ7exEbjg2QxMP4dQFNfDApIPjoW/buhK7kTEw4HLVM6oB5mK7VccW+1nXJpkFwmZm7njEabuUwm4SLBFjfxtV1f87UpTerBl8b" +
                    "ywnDshV1RK+nafumZMG0LB5l5KOFuRmD8HvG49s1kgT1ugm4MOts+2KEYDciD/sYRKqZeAp2CNc6cOFQ40jSOpUbf24auVv/hIOi" +
                    "zLy2KrBdhit+6rDAY5Uf9A+L7A41fVWNEGq0l6arTpz7zDHC23iQLttmvRLXNLLumw5XQhP9FEgEWmMFFMmUAB07IIj2XzGFkNTG" +
                    "pIYkXvEsi52Jusald+WNz+yA8Wi7KYbaf9mfIZSSCbHuy775VqYf++H8ojJ0nxmRcpPj4bhlUP+dxrN3MwdGBKYuE4P+6G7xpZcE" +
                    "js5DPJoeZLwP2u9BmmsDbU5RAGPRloUDv4En1TxnNortabAOBzce/SQVmaerc07SxjabVhPHjBZ7oj8/jec2EO7+tyF/AFBLAwQU" +
                    "AAAAAACgS1NdCXheKagBAACoAQAAPQAAAFdSUFJDIFJldm9jYXRpb24gTGlzdCBTaWduaW5nIENlcnRpZmljYXRlc1wgU3RhdHVz" +
                    "IFNpZ25lci5jZXIwggGkMIIBS6ADAgECAhRafEW9I4tPxpWzd1yB2vnXByAluzAKBggqhkjOPQQDAjAoMSYwJAYDVQQDDB1UZXN0" +
                    "IFdSUFJDIFN0YXR1cyBMaXN0IFNpZ25lcjAeFw0yNjEwMTkwOTM2MDBaFw0zNjEwMTYwOTM2MDBaMCgxJjAkBgNVBAMMHVRlc3Qg" +
                    "V1JQUkMgU3RhdHVzIExpc3QgU2lnbmVyMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEzbHn4BSnNMSkspo9Ax2j17BgjsLAzWdc" +
                    "WoEOyW0RP368TCc8fqniTHlbeQEKiJcdHCmwGzogqntyeKHY5wJaFKNTMFEwHQYDVR0OBBYEFGm7Nzwc5dRWfNr5NEJfKL+9W8qu" +
                    "MB8GA1UdIwQYMBaAFGm7Nzwc5dRWfNr5NEJfKL+9W8quMA8GA1UdEwEB/wQFMAMBAf8wCgYIKoZIzj0EAwIDRwAwRAIgegQakK6m" +
                    "jvj54f2/O0tkeBbNsiMqPTasZ2G2YkBIK3cCIHbym+th8mVKrUS1t6s8Wp164Nh1qlqoVwUTd8CVSRBFUEsDBBQAAAAIAKBLU10S" +
                    "D7IozQEAAHcCAAAdAAAAT3RoZXIgQ2VydGlmaWNhdGVzXFNpZ25lci5jZXJ9kkuPqkAQRvf8irs3ZgB11MUs+gW02Cjvxw5pRZGR" +
                    "9kUrv37Q3MzmJrdWlVMnlVTlGw77gsSkzh9EvIAaFIGAvOBQYZRCgREC4b0EkkJQ0jA+PYzWXy2DWR5dR7wo+WWiyaJcbK4SlSm1" +
                    "m4x2lUp6Xyo2Jt8MuSbQwh5IHplRxxFMwhr67hMG3DRUnrAyMOsXD/KY37OESmtfOKxKHwrDZMLwsXMqIOMX7NJHz/RfVoGG+alc" +
                    "gBRHrosx1MIscVRqcD80FlihhqOmiacVT8jy98ARmT6piwpmDDITPs+mz8ZzUBITob+9JBZQKYBMfyiTAQhFFxCxtPO1y3gz38qU" +
                    "TpFk2ikxzhBf6xsJ5oOWyNPa+hTj42MSj44QNYfE8qRQrvD6SW0xLU6Hvb4qULz3q7CDHoPq6y8cl24MoSfuXTGzVkkYnbp8EHgo" +
                    "eR4+2qi1lUsFdrB0IstnxMQgLv8j9+767fbrwW5GYACwAlzrg4Hm9z4IGAJqARhxEbUaYgp0F810PNha87a7LTOrjLsNrexK1S8m" +
                    "3yu37wXw0W0EDlCfifaS7ha1f9bIbbO9rI0z395xynORn43CIKMdqMOj67lfX8o7RsTB/0brB1BLAwQUAAAACACgS1NdrZ3VQhUA" +
                    "AAATAAAAKQAAAFdSUFJDIFByb3ZpZGVyIENBIENlcnRpZmljYXRlc1xyZWFkbWUudHh0y8tXSE4tKslMy0xOLElVyEgtSgUAUEsB" +
                    "AhQDFAAAAAgAoEtTXfzO8R1TAgAAPQMAAC0AAAAAAAAAAAAAAIABAAAAAFdSUFJDIFByb3ZpZGVyIENBIENlcnRpZmljYXRlc1xB" +
                    "LVNJVCBSb290LmNlclBLAQIUAxQAAAAAAKBLU10JeF4pqAEAAKgBAAA9AAAAAAAAAAAAAACAAZ4CAABXUlBSQyBSZXZvY2F0aW9u" +
                    "IExpc3QgU2lnbmluZyBDZXJ0aWZpY2F0ZXNcIFN0YXR1cyBTaWduZXIuY2VyUEsBAhQDFAAAAAgAoEtTXRIPsijNAQAAdwIAAB0A" +
                    "AAAAAAAAAAAAAIABoQQAAE90aGVyIENlcnRpZmljYXRlc1xTaWduZXIuY2VyUEsBAhQDFAAAAAgAoEtTXa2d1UIVAAAAEwAAACkA" +
                    "AAAAAAAAAAAAAIABqQYAAFdSUFJDIFByb3ZpZGVyIENBIENlcnRpZmljYXRlc1xyZWFkbWUudHh0UEsFBgAAAAAEAAQAaAEAAAUH" +
                    "AAAAAA=="
    }
}
