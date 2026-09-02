package at.asitplus.wallet.app.common.relyingParty.validation.certificates

import at.asitplus.catchingUnwrapped
import at.asitplus.iso.sha256
import at.asitplus.signum.indispensable.asn1.Asn1EncapsulatingOctetString
import at.asitplus.signum.indispensable.asn1.Asn1Primitive
import at.asitplus.signum.indispensable.asn1.Asn1Sequence
import at.asitplus.signum.indispensable.asn1.ObjectIdentifier
import at.asitplus.signum.indispensable.asn1.TagClass
import at.asitplus.signum.indispensable.cosef.io.Base16Strict
import at.asitplus.signum.indispensable.pki.CertificateChain
import at.asitplus.signum.indispensable.pki.X509Certificate
import at.asitplus.signum.indispensable.requireSupported
import at.asitplus.signum.supreme.sign.SignatureInput
import at.asitplus.signum.supreme.sign.verifierFor
import at.asitplus.wallet.app.common.asitRootPem
import io.github.aakira.napier.Napier
import io.matthewnelson.encoding.core.Encoder.Companion.encodeToString
import kotlin.time.Clock
import kotlin.time.Instant

class WrpChainValidator {
    fun validateChain(chain: CertificateChain?, source: String): Boolean {
        val tag = tagFor(source)
        if (chain.isNullOrEmpty()) {
            Napier.w("$source is missing x5c certificate chain.", tag = tag)
            return false
        }
        Napier.d("$source: received x5c chain with ${chain.size} certificate(s).", tag = tag)
        val now = Clock.System.now()
        chain.forEachIndexed { idx, certificate ->
            Napier.d("$source[$idx]: ${certificateSummary(certificate)}", tag = tag)
        }

        if (!validateValidityPeriods(chain, source, now)) {
            return false
        }

        val signatureChainOk = validateSignatures(chain, source)
        val trustedRootOk = validateTrustedRoot(chain, source)
        if (!signatureChainOk || !trustedRootOk) {
            Napier.w("$source: x5c validation failed.", tag = tag)
            return false
        }

        Napier.d("$source: x5c validation completed (full chain validity enforced).", tag = tag)
        return true
    }

    private fun validateValidityPeriods(chain: CertificateChain, source: String, now: Instant): Boolean {
        val tag = tagFor(source)
        chain.forEachIndexed { idx, certificate ->
            val validFrom = certificate.tbsCertificate.validFrom.instant
            val validUntil = certificate.tbsCertificate.validUntil.instant
            Napier.d("$source[$idx]: validity=$validFrom .. $validUntil", tag = tag)
            if (now < validFrom) {
                Napier.w("$source[$idx] is not yet valid (valid from $validFrom).", tag = tag)
                return false
            }
            if (now > validUntil) {
                Napier.w("$source[$idx] is expired (valid until $validUntil).", tag = tag)
                return false
            }
        }
        return true
    }

    private fun validateSignatures(chain: CertificateChain, source: String): Boolean {
        val tag = tagFor(source)
        if (chain.size == 1) {
            Napier.d(
                "$source has only leaf certificate in x5c; issuer signature check delegated to trust anchor.", tag = tag
            )
            return true
        }

        chain.windowed(size = 2, step = 1).forEachIndexed { idx, pair ->
            val child = pair[0]
            val issuer = pair[1]
            if (!isCertificateSignedBy(child, issuer)) {
                Napier.e(
                    "$source[$idx] is not signed by $source[${idx + 1}]. " + "child=${shortFingerprint(child)}, issuer=${
                        shortFingerprint(
                            issuer
                        )
                    }, " + "childAki=${shortHex(extractAuthorityKeyIdentifier(child))}, " + "issuerSki=${
                        shortHex(
                            extractSubjectKeyIdentifier(issuer)
                        )
                    }", tag = tag
                )
                return false
            }
        }

        return true
    }

    private fun validateTrustedRoot(chain: CertificateChain, source: String): Boolean {
        val tag = tagFor(source)
        val trustedRoots = loadRequestCertificateTrustAnchors()
        if (trustedRoots.isEmpty()) {
            Napier.e("No trusted root certificates configured for request validation.", tag = tag)
            return false
        }
        Napier.d(
            "$source: checking x5c top certificate against ${trustedRoots.size} trusted root certificate(s).", tag = tag
        )

        val chainTop = chain.last()
        val anchored = trustedRoots.any { trustedRoot ->
            val sameCertificate = areSameCertificate(chainTop, trustedRoot)
            val signedByTrustedRoot = !sameCertificate && isCertificateSignedBy(chainTop, trustedRoot)
            if (sameCertificate || signedByTrustedRoot) {
                Napier.d(
                    "$source: trust anchor matched trusted root (${shortFingerprint(trustedRoot)}), " + "mode=${if (sameCertificate) "exact" else "signed"}.",
                    tag = tag
                )
            }
            sameCertificate || signedByTrustedRoot
        }

        if (!anchored) {
            Napier.e(
                "$source is not anchored to a configured trusted root certificate. " + "x5cTop=${
                    shortFingerprint(
                        chainTop
                    )
                }", tag = tag
            )
            return false
        }

        Napier.d("$source: x5c chain anchored to trusted roots.", tag = tag)
        return true
    }

    private fun isCertificateSignedBy(certificate: X509Certificate, issuer: X509Certificate): Boolean =
        catchingUnwrapped {
            val signatureAlgorithm = certificate.signatureAlgorithm
            signatureAlgorithm.requireSupported()
            val issuerPublicKey = issuer.decodedPublicKey.getOrThrow()
            val verifier = signatureAlgorithm.verifierFor(issuerPublicKey).getOrThrow()
            val tbsBytes = certificate.tbsCertificate.encodeToDer()
            val signature = certificate.decodedSignature.getOrThrow()
            verifier.verify(SignatureInput(tbsBytes), signature).getOrThrow()
            true
        }.getOrDefault(false)

    private fun areSameCertificate(first: X509Certificate, second: X509Certificate): Boolean =
        catchingUnwrapped { first.encodeToDer().contentEquals(second.encodeToDer()) }.getOrDefault(false)

    private fun certificateSummary(certificate: X509Certificate): String {
        val serial = catchingUnwrapped {
            certificate.tbsCertificate.serialNumber.encodeToString(Base16Strict)
        }.getOrDefault("n/a")
        val fingerprint = shortFingerprint(certificate)
        val aki = shortHex(extractAuthorityKeyIdentifier(certificate))
        val ski = shortHex(extractSubjectKeyIdentifier(certificate))
        val subject = catchingUnwrapped { certificate.tbsCertificate.subjectName.toString() }.getOrDefault("n/a")
        val issuer = catchingUnwrapped { certificate.tbsCertificate.issuerName.toString() }.getOrDefault("n/a")
        return "serial=$serial, sha256=$fingerprint, aki=$aki, ski=$ski, subject=$subject, issuer=$issuer"
    }

    private fun shortFingerprint(certificate: X509Certificate): String = catchingUnwrapped {
        val full = certificate.encodeToDer().sha256().encodeToString(Base16Strict)
        if (full.length <= 24) full else full.take(24) + "..."
    }.getOrDefault("n/a")

    private fun extractAuthorityKeyIdentifier(certificate: X509Certificate): ByteArray? = catchingUnwrapped {
        val extension =
            certificate.tbsCertificate.extensions?.firstOrNull { it.oid == AUTHORITY_KEY_IDENTIFIER_OID } ?: return null
        val authorityKeyIdentifier =
            (extension.value as? Asn1EncapsulatingOctetString)?.children?.firstOrNull() as? Asn1Sequence ?: return null
        val keyIdentifier = authorityKeyIdentifier.children.firstOrNull {
            it.tag.tagClass == TagClass.CONTEXT_SPECIFIC && it.tag.tagValue == 0uL && it is Asn1Primitive
        } as? Asn1Primitive ?: return null
        keyIdentifier.content
    }.getOrNull()

    private fun extractSubjectKeyIdentifier(certificate: X509Certificate): ByteArray? = catchingUnwrapped {
        val extension =
            certificate.tbsCertificate.extensions?.firstOrNull { it.oid == SUBJECT_KEY_IDENTIFIER_OID } ?: return null
        ((extension.value as? Asn1EncapsulatingOctetString)?.children?.firstOrNull() as? Asn1Primitive)?.content
    }.getOrNull()

    private fun shortHex(bytes: ByteArray?): String = bytes?.let {
        val full = it.encodeToString(Base16Strict)
        if (full.length <= 24) full else full.take(24) + "..."
    } ?: "n/a"

    private fun tagFor(source: String): String = when {
        source.startsWith("WRPRC") -> TAG_WRPRC
        source.startsWith("WRPAC") -> TAG_WRPAC
        else -> TAG_CHAIN_VALIDATOR
    }

    fun loadRequestCertificateTrustAnchors(): List<X509Certificate> =
        requestCertificateTrustAnchors.mapNotNull { X509Certificate.decodeFromPem(it).getOrNull() }

    val requestCertificateTrustAnchors = listOf(asitRootPem)

    private companion object Constants {
        val TAG_CHAIN_VALIDATOR = "WrpChainValidator"
        val TAG_WRPRC = "$TAG_CHAIN_VALIDATOR[WRPRC]"
        val TAG_WRPAC = "$TAG_CHAIN_VALIDATOR[WRPAC]"
        val AUTHORITY_KEY_IDENTIFIER_OID = ObjectIdentifier("2.5.29.35")
        val SUBJECT_KEY_IDENTIFIER_OID = ObjectIdentifier("2.5.29.14")
    }
}
