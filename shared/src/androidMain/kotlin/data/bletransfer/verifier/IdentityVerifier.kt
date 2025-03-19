package data.bletransfer.verifier

import at.asitplus.signum.indispensable.cosef.CoseSigned
import at.asitplus.signum.indispensable.jcaSignatureBytes
import com.android.identity.cbor.Bstr
import com.android.identity.cbor.Cbor
import com.android.identity.cbor.Cbor.encode
import com.android.identity.cbor.CborArray
import com.android.identity.cbor.CborMap
import com.android.identity.cbor.DataItem
import com.android.identity.cbor.RawCbor
import com.android.identity.cbor.Tagged
import com.android.identity.cbor.toDataItem
import com.android.identity.cose.Cose
import com.android.identity.crypto.Algorithm
import com.android.identity.crypto.Crypto
import com.android.identity.crypto.EcPublicKey
import com.android.identity.crypto.EcSignature
import com.android.identity.crypto.X509Cert
import com.android.identity.crypto.X509CertChain
import com.android.identity.crypto.javaX509Certificate
import data.bletransfer.util.RequestedDocument
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.Signature
import java.security.SignatureException


object IdentityVerifier {

     fun verifyReaderIdentity(requestedDocument: RequestedDocument,
                              coseSigned: CoseSigned<ByteArray>,
                              sessionTranscript: ByteArray?) {
         if (sessionTranscript != null) {
             val readerAuthBytes = buildReaderAuthenticationBytes(requestedDocument, sessionTranscript)
//             val data = coseSigned.prepareCoseSignatureInput(detachedPayload = readerAuthBytes)
             println("SRKI${readerAuthBytes}")
             println("SRKI${coseSigned.payload}")
             println("SRKI${readerAuthBytes.contentEquals(coseSigned.payload)}")

             val chain: X509CertChain? = coseSigned.unprotectedHeader?.certificateChain?.let {
                 X509CertChain.fromDataItem(
                     it.toDataItem())
             }
             val appCert = chain?.certificates?.get(0)
             println("SRKI${coseSigned.payload}")
             appCert?.ecPublicKey?.let {
                 println("SRKI:${verifySignature(coseSigned.prepareCoseSignatureInput(), EcSignature.fromCoseEncoded(coseSigned.wireFormat.rawSignature),
                     it, Algorithm.ES256)}")
             }
//             appCert?.ecPublicKey?.let {
//                 Cose.coseSign1Check(
//                     it,
//                     null,
//                     coseSigned,
//                     Algorithm.ES256
//                 )
//             }
         }

//        TODO build readerAuthenticationBytes for signature (see DeviceRequestGenerator)
//        TODO extract certificate from cosesign1 and use public key to verify signature against readerAuthenticationBytes
//        TODO verify certificate, mock seal since currently wallet is receiving only app cert

    }

    private fun buildReaderAuthenticationBytes(requestedDocument: RequestedDocument,
                                               sessionTranscript: ByteArray) : ByteArray{

        val itemsToRequest: Map<String, Map<String, Boolean>> = requestedDocument.nameSpaces.associate { ns ->
            ns.nameSpace to ns.attributesMap.mapKeys { it.key.value }
        }
        println("SRKI:${itemsToRequest}")
        println("SRKI:${requestedDocument.nameSpaces[0].attributesMap}")

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


    fun verifySignature(
        data: ByteArray,
        digitalSignature: EcSignature,
        publicKey: EcPublicKey,
        algorithm: Algorithm
    ): Boolean {
        return Crypto.checkSignature(publicKey, data, algorithm, digitalSignature)
    }


}