package data.bletransfer.verifier

import at.asitplus.signum.indispensable.cosef.CoseSigned
import data.bletransfer.util.RequestedDocument

class IdentityVerifier {

    fun verifyReaderIdentity(requestedDocument: RequestedDocument, coseSigned: CoseSigned<ByteArray>) {
//        TODO build readerAuthenticationBytes for signature (see DeviceRequestGenerator)
//        TODO extract certificate from cosesign1 and use public key to verify signature against readerAuthenticationBytes
//        TODO verify certificate, mock seal since currently wallet is receiving only app cert

    }

}