package data.bletransfer.verifier

import android.content.Context
import at.asitplus.signum.indispensable.cosef.CoseSigned
import com.android.identity.cbor.Bstr
import com.android.identity.cbor.Cbor.encode
import com.android.identity.cbor.CborArray
import com.android.identity.cbor.CborMap
import com.android.identity.cbor.DataItem
import com.android.identity.cbor.RawCbor
import com.android.identity.cbor.Tagged
import com.android.identity.cbor.toDataItem
import com.android.identity.crypto.Algorithm
import com.android.identity.crypto.Crypto
import com.android.identity.crypto.EcSignature
import com.android.identity.crypto.X509Cert
import com.android.identity.crypto.X509CertChain
import com.android.identity.crypto.javaX509Certificate
import data.bletransfer.util.RequestedDocument
import data.trustlist.AndroidTrustListService
import data.trustlist.TrustListService
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.asn1.x509.DigestInfo
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.cert.CertPathValidatorException
import java.security.cert.X509Certificate
import java.security.interfaces.ECPublicKey
import java.util.Date
import java.util.Locale
import kotlin.collections.component1
import kotlin.collections.component2


object IdentityVerifier {

    private val ALLOWED_ALGOS = setOf("1.2.840.10045.4.3.2", "1.2.840.10045.4.3.3", "1.2.840.10045.4.3.4")
    private const val KEY_USAGE_DIGITAL_SIGNATURE: Int = 0

    var requesterIdentity: Map<String, String> = emptyMap()
    var fingerprintTrustList: List<String> = emptyList()

     fun verifyReaderIdentity(requestedDocument: RequestedDocument,
                              coseSigned: CoseSigned<ByteArray>,
                              sessionTranscript: ByteArray?,
                              context: Context): Boolean {
         try {
             if (sessionTranscript == null) return false

             val certList: List<X509Cert>? = coseSigned.unprotectedHeader?.certificateChain?.mapNotNull { byteArray ->
                 try {
                     X509Cert.fromDataItem(byteArray.toDataItem())
                 } catch (e: Exception) {
                     return false
                 }
             }
             if (certList?.size != 2) return false

             val readerAuthBytes =
                 buildReaderAuthenticationBytes(requestedDocument, sessionTranscript)
             val data = coseSigned.prepareCoseSignatureInput(detachedPayload = readerAuthBytes)


             requesterIdentity = parseDn(certList[1].javaX509Certificate.subjectX500Principal.name)
             val location =
                 listOfNotNull(requesterIdentity["L"], requesterIdentity["C"]).joinToString(", ")
             if (location.isNotEmpty()) {
                 requesterIdentity = requesterIdentity + ("Loc" to location)
             }

             verifyCertificateChain(
                 certList.map { it.javaX509Certificate }
             )

             if (!Crypto.checkSignature(
                     certList[0].ecPublicKey, data, Algorithm.ES256,
                     EcSignature.fromCoseEncoded(coseSigned.wireFormat.rawSignature)
                 )
             ) return false

             return isSealCertTrusted(certList[1].javaX509Certificate, context)
         } catch (e: Exception) {
             return false
         }
     }

    private fun verifyCertificateChain(certificateChain: List<X509Certificate>) {
        certificateChain.forEach {
            it.checkValidity()
            verifySignatureAlgo(it)
            verifyCriticalExtensions(it)
            certHasDigitalSignatureKeyUsage(it)
        }

        val appCert = certificateChain[0]
        val sealCert = certificateChain[1]
        wasCertificateIssuedWithinIssuerValidityPeriod(appCert.notBefore, sealCert)
        appCert.verify(sealCert.publicKey)
        subjectAndIssuerPrincipalMatch(appCert, sealCert)
    }

    private fun certHasDigitalSignatureKeyUsage(cert: X509Certificate) {
        val keyUsage = cert.keyUsage
        if (keyUsage == null || !keyUsage[KEY_USAGE_DIGITAL_SIGNATURE]) {
            throw CertPathValidatorException("Digital signature key usage is not set.")
        }
    }

    private fun subjectAndIssuerPrincipalMatch(
        childCert: X509Certificate,
        issuerCert: X509Certificate
    ) {
        val issuerInChildPrincipal: Map<String, String> = parseDn(childCert.issuerX500Principal.name)
        val subjectInIssuerPrincipal: Map<String, String> = parseDn(issuerCert.subjectX500Principal.name)
        if (issuerInChildPrincipal != subjectInIssuerPrincipal) {
            throw CertPathValidatorException("subject of issuer cert and issuer of child certificate mismatch.")
        }
    }

    private fun verifyCriticalExtensions(cert: X509Certificate) {
        if (cert.hasUnsupportedCriticalExtension()) {
            throw CertPathValidatorException("Certificate has unsupported critical extensions.")
        }
    }

    private fun wasCertificateIssuedWithinIssuerValidityPeriod(
        dateOfIssuance: Date,
        issuerCert: X509Certificate
    ) {
        val beginValidity = issuerCert.notBefore
        val endValidity = issuerCert.notAfter
        if (beginValidity.after(dateOfIssuance) || dateOfIssuance.after(endValidity)) {
            throw CertPathValidatorException("Certificate issued outside issuer validity period.")
        }
    }

    private fun verifySignatureAlgo(certificate: X509Certificate) {
        if (!ALLOWED_ALGOS.contains(certificate.sigAlgOID)) {
            throw NoSuchAlgorithmException("Unsupported signature algorithm.")
        }
        val publicKey = certificate.publicKey
        if ((publicKey as ECPublicKey).params.curve.field.fieldSize < 256) {
            throw RuntimeException("Unsatisfactory EC key size.")
        }
    }

    private fun isSealCertTrusted(sealCertificate: X509Certificate, context: Context): Boolean {
        val trustListService: TrustListService = AndroidTrustListService()
        trustListService.setContext(context)
        fingerprintTrustList = trustListService.getTrustedFingerprints() ?: emptyList()
        return fingerprintTrustList.contains(sealCertificate.toFingerprint())
    }


    private fun buildReaderAuthenticationBytes(requestedDocument: RequestedDocument,
                                               sessionTranscript: ByteArray) : ByteArray{
//        TODO consider adding requestInfo in the process, currently assuming that requestInfo is null
        val itemsToRequest: Map<String, Map<String, Boolean>> = requestedDocument.nameSpaces.associate { ns ->
            ns.nameSpace to ns.attributesMap.mapKeys { it.key.value }
        }
        val nsBuilder = CborMap.builder().apply {
            for ((namespaceName, innerMap) in itemsToRequest) {
                putMap(namespaceName).let { elemBuilder ->
                    for ((elemName, intentToRetain) in innerMap) {
                        elemBuilder.put(elemName, intentToRetain)
                    }
                    elemBuilder.end()
                }
            }
        }
        nsBuilder.end()

        val irMapBuilder = CborMap.builder().apply {
            put("docType", requestedDocument.docType)
            put("nameSpaces", nsBuilder.end().build())
        }
        irMapBuilder.end()
        val encodedItemsRequest = encode(irMapBuilder.end().build())
        val itemsRequestBytesDataItem: DataItem = Tagged(24, Bstr(encodedItemsRequest))

        val encodedReaderAuthentication = encode(
            CborArray.builder()
                .add("ReaderAuthentication")
                .add(RawCbor(sessionTranscript))
                .add(itemsRequestBytesDataItem)
                .end()
                .build()
        )
        return encode(Tagged(24, Bstr(encodedReaderAuthentication)))
    }

    private fun parseDn(dn: String): Map<String, String> {
        return dn.split(",")
            .map { it.trim() }
            .mapNotNull { parseRdn(it) }
            .toMap()
    }

    private fun parseRdn(rdn: String): Pair<String, String>? {
        val parts = rdn.split("=")
        return if (parts.size == 2) {
            val key = parts[0].trim().uppercase(Locale.getDefault())  // Normalize the key
            val value = parts[1].trim()
            key to value
        } else {
            null
        }
    }

    private fun X509Certificate.toFingerprint(): String {
        val md = MessageDigest.getInstance("SHA-256")
        val messageHash = md.digest(encoded)

        val hashAlgorithmFinder = DefaultDigestAlgorithmIdentifierFinder()
        val hashingAlgorithmIdentifier: AlgorithmIdentifier = hashAlgorithmFinder.find("SHA-256")
        val digestInfo = DigestInfo(hashingAlgorithmIdentifier, messageHash)

        return String(digestInfo.encoded)
    }
}