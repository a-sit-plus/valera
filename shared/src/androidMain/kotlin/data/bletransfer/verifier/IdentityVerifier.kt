package data.bletransfer.verifier

import android.content.Context
import androidx.compose.runtime.Composable
import at.asitplus.signum.indispensable.cosef.CoseSigned
import at.asitplus.wallet.app.common.HttpService
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
import com.android.identity.crypto.X509CertChain
import com.android.identity.crypto.javaX509Certificate
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import data.bletransfer.util.RequestedDocument
import data.storage.CertificateStorage
import data.trustlist.AndroidTrustListService
import data.trustlist.TrustListService
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.asn1.x509.DigestInfo
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SignatureException
import java.security.cert.CertPathValidatorException
import java.security.cert.CertificateException
import java.security.cert.X509Certificate


object IdentityVerifier {

    private val ALLOWED_ALGOS = setOf("1.2.840.10045.4.3.2", "1.2.840.10045.4.3.3", "1.2.840.10045.4.3.4")

    var requesterIdentity: String? = null
    var fingerprintTrustList: List<String> = emptyList()

     fun verifyReaderIdentity(requestedDocument: RequestedDocument,
                              coseSigned: CoseSigned<ByteArray>,
                              sessionTranscript: ByteArray?,
                              context: Context): Boolean {
         if (sessionTranscript == null) return false

         val readerAuthBytes = buildReaderAuthenticationBytes(requestedDocument, sessionTranscript)
         val data = coseSigned.prepareCoseSignatureInput(detachedPayload = readerAuthBytes)

         val appCertificate = coseSigned.unprotectedHeader?.certificateChain?.let {
             X509CertChain.fromDataItem(it.toDataItem())
         }?.certificates?.firstOrNull() ?: return false


         requesterIdentity = extractCommonName(appCertificate.javaX509Certificate.subjectX500Principal.name)
         val seal = CertificateStorage.loadCertificateAndroid(context, "SEAL") ?: return false

         verifyCertificateChain(listOf(appCertificate.javaX509Certificate, seal.javaX509Certificate))

         if (!Crypto.checkSignature(
                 appCertificate.ecPublicKey, data, Algorithm.ES256,
                 EcSignature.fromCoseEncoded(coseSigned.wireFormat.rawSignature))) return false

         return isSealCertTrusted(seal.javaX509Certificate, context)

     }

    private fun verifyCertificateChain(certificateChain: List<X509Certificate>) {
        certificateChain.forEach {
            it.checkValidity()
            if (!ALLOWED_ALGOS.contains(it.sigAlgOID)) {
                throw NoSuchAlgorithmException("Unsupported signature algorithm.")
            }
            if (it.hasUnsupportedCriticalExtension()) {
                throw CertificateException("Certificate has unsupported critical extensions.")
            }
        }

        certificateChain[0].verify(certificateChain[1].publicKey)

        if (!certificateChain[1].subjectX500Principal.equals(certificateChain[0].issuerX500Principal)) {
            throw CertPathValidatorException("CA subject and issued certificate issuer mismatch!");
        }
        val certSignUsage = 5
        if (certificateChain[1].keyUsage == null || !certificateChain[1].keyUsage[certSignUsage]) {
            throw SignatureException("Signing certificates key usage extension not present in SEAL!");
        }
        if (certificateChain[0].notBefore.before(certificateChain[1].notBefore) ||
            certificateChain[0].notAfter.after(certificateChain[1].notAfter)) {
            throw CertificateException("App Cert is not issued during SEAL validity period.");
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
//        TODO add requestInfo in the process
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

    private fun extractCommonName(dn: String): String? {
        val cnRegex = "CN=([^,]+)".toRegex()
        return cnRegex.find(dn)?.groups?.get(1)?.value
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