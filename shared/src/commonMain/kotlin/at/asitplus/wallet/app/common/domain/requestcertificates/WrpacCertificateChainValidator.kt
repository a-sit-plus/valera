package at.asitplus.wallet.app.common.domain.requestcertificates

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
import io.github.aakira.napier.Napier
import io.matthewnelson.encoding.core.Encoder.Companion.encodeToString
import kotlin.time.Clock

internal class WrpacCertificateChainValidator {
    private val authorityKeyIdentifierOid = ObjectIdentifier("2.5.29.35")
    private val subjectKeyIdentifierOid = ObjectIdentifier("2.5.29.14")

    fun validateMandatoryChain(chain: CertificateChain?, source: String): X509Certificate? {
        val tag = tagFor(source)
        if (chain.isNullOrEmpty()) {
            Napier.e("$source is missing x5c certificate chain.", tag = tag)
            return null
        }
        Napier.d("$source: received x5c chain with ${chain.size} certificate(s).", tag = tag)
        return validateChain(chain, source)
    }

    fun validateOptionalChain(chain: CertificateChain, source: String): X509Certificate? =
        validateChain(chain, source).also {
            if (it != null) {
                Napier.d("$source: optional x5c chain accepted.", tag = tagFor(source))
            }
        }

    private fun validateChain(chain: CertificateChain, source: String): X509Certificate? {
        val tag = tagFor(source)
        val now = Clock.System.now()
        val leaf = chain.first()
        val leafValidFrom = leaf.tbsCertificate.validFrom.instant
        val leafValidUntil = leaf.tbsCertificate.validUntil.instant
        Napier.d("$source[0]: validity=$leafValidFrom .. $leafValidUntil", tag = tag)
        Napier.d("$source[0]: ${certificateSummary(leaf)}", tag = tag)
        if (now < leafValidFrom) {
            Napier.e("$source[0] is not yet valid (valid from $leafValidFrom).", tag = tag)
            return null
        }
        if (now > leafValidUntil) {
            Napier.e("$source[0] is expired (valid until $leafValidUntil).", tag = tag)
            return null
        }

        if (chain.size > 1) {
            chain.drop(1).forEachIndexed { offset, certificate ->
                val idx = offset + 1
                val validFrom = certificate.tbsCertificate.validFrom.instant
                val validUntil = certificate.tbsCertificate.validUntil.instant
                Napier.d("$source[$idx]: validity=$validFrom .. $validUntil", tag = tag)
                Napier.d("$source[$idx]: ${certificateSummary(certificate)}", tag = tag)
            }
        }

        val signatureChainOk = validateSignatures(chain, source)
        val trustedRootOk = validateTrustedRoot(chain, source)
        if (!signatureChainOk || !trustedRootOk) {
            Napier.e("$source: x5c validation failed.", tag = tag)
            return null
        }

        Napier.d("$source: x5c validation completed (leaf enforced).", tag = tag)
        return leaf
    }

    private fun validateSignatures(chain: CertificateChain, source: String): Boolean {
        val tag = tagFor(source)
        if (chain.size == 1) {
            Napier.d("$source has only leaf certificate in x5c; issuer signature check delegated to trust anchor.", tag = tag)
            return true
        }

        chain.windowed(size = 2, step = 1).forEachIndexed { idx, pair ->
            val child = pair[0]
            val issuer = pair[1]
            if (!isCertificateSignedBy(child, issuer)) {
                Napier.e(
                    "$source[$idx] is not signed by $source[${idx + 1}]. " +
                            "child=${shortFingerprint(child)}, issuer=${shortFingerprint(issuer)}, " +
                            "childAki=${shortHex(extractAuthorityKeyIdentifier(child))}, " +
                            "issuerSki=${shortHex(extractSubjectKeyIdentifier(issuer))}",
                    tag = tag
                )
                return false
            }
        }

        return true
    }

    private fun validateTrustedRoot(chain: CertificateChain, source: String): Boolean {
        val tag = tagFor(source)
        val trustedRoots = RequestCertificateTrustAnchors.current()
        if (trustedRoots.isEmpty()) {
            Napier.e("No trusted root certificates configured for request validation.", tag = tag)
            return false
        }
        Napier.d("$source: checking x5c top certificate against ${trustedRoots.size} trusted root certificate(s).", tag = tag)

        val chainTop = chain.last()
        val anchored = trustedRoots.any { trustedRoot ->
            val sameCertificate = areSameCertificate(chainTop, trustedRoot)
            val signedByTrustedRoot = !sameCertificate && isCertificateSignedBy(chainTop, trustedRoot)
            if (sameCertificate || signedByTrustedRoot) {
                Napier.d(
                    "$source: trust anchor matched trusted root (${shortFingerprint(trustedRoot)}), " +
                            "mode=${if (sameCertificate) "exact" else "signed"}.",
                    tag = tag
                )
            }
            sameCertificate || signedByTrustedRoot
        }

        if (!anchored) {
            Napier.e(
                "$source is not anchored to a configured trusted root certificate. " +
                        "x5cTop=${shortFingerprint(chainTop)}",
                tag = tag
            )
            return false
        }

        Napier.d("$source: x5c chain anchored to trusted roots.", tag = tag)
        return true
    }

    private fun isCertificateSignedBy(certificate: X509Certificate, issuer: X509Certificate): Boolean =
        runCatching {
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
        runCatching { first.encodeToDer().contentEquals(second.encodeToDer()) }.getOrDefault(false)

    private fun certificateSummary(certificate: X509Certificate): String {
        val serial = runCatching {
            certificate.tbsCertificate.serialNumber.encodeToString(Base16Strict)
        }.getOrDefault("n/a")
        val fingerprint = shortFingerprint(certificate)
        val aki = shortHex(extractAuthorityKeyIdentifier(certificate))
        val ski = shortHex(extractSubjectKeyIdentifier(certificate))
        val subject = runCatching { certificate.tbsCertificate.subjectName.toString() }.getOrDefault("n/a")
        val issuer = runCatching { certificate.tbsCertificate.issuerName.toString() }.getOrDefault("n/a")
        return "serial=$serial, sha256=$fingerprint, aki=$aki, ski=$ski, subject=$subject, issuer=$issuer"
    }

    private fun shortFingerprint(certificate: X509Certificate): String =
        runCatching {
            val full = certificate.encodeToDer().sha256().encodeToString(Base16Strict)
            if (full.length <= 24) full else full.take(24) + "..."
        }.getOrDefault("n/a")

    private fun extractAuthorityKeyIdentifier(certificate: X509Certificate): ByteArray? =
        runCatching {
            val extension = certificate.tbsCertificate.extensions
                ?.firstOrNull { it.oid == authorityKeyIdentifierOid }
                ?: return null
            val authorityKeyIdentifier = (extension.value as? Asn1EncapsulatingOctetString)
                ?.children
                ?.firstOrNull() as? Asn1Sequence ?: return null
            val keyIdentifier = authorityKeyIdentifier.children
                .firstOrNull {
                    it.tag.tagClass == TagClass.CONTEXT_SPECIFIC &&
                            it.tag.tagValue == 0uL &&
                            it is Asn1Primitive
                } as? Asn1Primitive ?: return null
            keyIdentifier.content
        }.getOrNull()

    private fun extractSubjectKeyIdentifier(certificate: X509Certificate): ByteArray? =
        runCatching {
            val extension = certificate.tbsCertificate.extensions
                ?.firstOrNull { it.oid == subjectKeyIdentifierOid }
                ?: return null
            ((extension.value as? Asn1EncapsulatingOctetString)
                ?.children
                ?.firstOrNull() as? Asn1Primitive)?.content
        }.getOrNull()

    private fun shortHex(bytes: ByteArray?): String =
        bytes?.let {
            val full = it.encodeToString(Base16Strict)
            if (full.length <= 24) full else full.take(24) + "..."
        } ?: "n/a"

    private fun tagFor(source: String): String = when {
        source.startsWith("WRPRC") -> "WrpacCertificateChainValidator[WRPRC]"
        source.startsWith("WRPAC") -> "WrpacCertificateChainValidator[WRPAC]"
        else -> "WrpacCertificateChainValidator"
    }
}
