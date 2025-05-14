package at.asitplus.wallet.app.common

import at.asitplus.rqes.QtspSignatureResponse
import at.asitplus.rqes.SignDocResponseParameters
import at.asitplus.rqes.SignHashResponseParameters
import at.asitplus.rqes.collection_entries.OAuthDocumentDigest
import at.asitplus.signum.indispensable.X509SignatureAlgorithm
import at.asitplus.signum.indispensable.asn1.ObjectIdentifier
import at.asitplus.signum.indispensable.asn1.ObjectIdentifierStringSerializer
import at.asitplus.signum.indispensable.io.ByteArrayBase64Serializer
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.rqes.RqesOpenId4VpHolder
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DocumentWithLabel(
    val document: ByteArray,
    val label: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        other as DocumentWithLabel

        if (!document.contentEquals(other.document)) return false
        if (label != other.label) return false

        return true
    }

    override fun hashCode(): Int {
        var result = document.contentHashCode()
        result = 31 * result + label.hashCode()
        return result
    }
}

/**
 * COPIED FROM QTSP SCA
 *
 *
 * Document to sign and signing parameters that are required to generate
 * data-to-be-signed representation.
 */
@Serializable
data class DataToBeSigned(
    /**
     * Document to sign.
     */
    @SerialName("document")
    @Serializable(with = ByteArrayBase64Serializer::class)
    val document: ByteArray,

    @SerialName("signingAlgorithmOid")
    @Serializable(with = ObjectIdentifierStringSerializer::class)
    val signingAlgorithmOid: ObjectIdentifier? = null,

    @SerialName("signingAlgorithParams")
    val signingAlgorithParams: ByteArray? = null,

    @SerialName("hashMethodOid")
    val hashMethod: String? = null,

    @SerialName("encMethodOid")
    val encMethod: String? = null,

    @SerialName("certificate")
    val certificate: ByteArray,

    @SerialName("certificateChain")
    val certificateChain: List<ByteArray>? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        other as DataToBeSigned

        if (!document.contentEquals(other.document)) return false
        if (signingAlgorithmOid != other.signingAlgorithmOid) return false
        if (signingAlgorithParams != null) {
            if (other.signingAlgorithParams == null) return false
            if (!signingAlgorithParams.contentEquals(other.signingAlgorithParams)) return false
        } else if (other.signingAlgorithParams != null) return false
        if (hashMethod != other.hashMethod) return false
        if (encMethod != other.encMethod) return false
        if (!certificate.contentEquals(other.certificate)) return false
        if (certificateChain != other.certificateChain) return false

        return true
    }

    override fun hashCode(): Int {
        var result = document.contentHashCode()
        result = 31 * result + (signingAlgorithmOid?.hashCode() ?: 0)
        result = 31 * result + (signingAlgorithParams?.contentHashCode() ?: 0)
        result = 31 * result + (hashMethod?.hashCode() ?: 0)
        result = 31 * result + (encMethod?.hashCode() ?: 0)
        result = 31 * result + certificate.contentHashCode()
        result = 31 * result + (certificateChain?.hashCode() ?: 0)
        return result
    }
}

/**
 * Helper data class for `/qtsp/sca/finalize` endpoint
 */
@Serializable
data class WrappedSignature(
    @SerialName("signature")
    val signature: String,
    @SerialName("transactionToken")
    val transactionToken: String,
)

@Serializable
data class FinishedDocument(
    @SerialName("document")
    @Serializable(with = ByteArrayBase64Serializer::class)
    val document: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        other as FinishedDocument

        return document.contentEquals(other.document)
    }

    override fun hashCode(): Int {
        return document.contentHashCode()
    }
}

@Serializable
data class DtbsrWrapper(
    @SerialName("dtbsR")
    @Serializable(with = ByteArrayBase64Serializer::class)
    val dtbsr: ByteArray,
    @SerialName("transactionToken")
    val transactionToken: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        other as DtbsrWrapper

        if (!dtbsr.contentEquals(other.dtbsr)) return false
        if (transactionToken != other.transactionToken) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dtbsr.contentHashCode()
        result = 31 * result + transactionToken.hashCode()
        return result
    }
}

//TODO probably moves into VCK when switching to internal processing
//For now use QTSP external SCA
internal suspend fun getDTBSR(
    client: HttpClient,
    qtspHost: String,
    signingCredential: RqesOpenId4VpHolder.SigningCredential,
    signatureAlgorithm: X509SignatureAlgorithm,
    document: DocumentWithLabel,
): Pair<String, OAuthDocumentDigest> {
    val dtbs = DataToBeSigned(
        document = document.document,
        certificate = signingCredential.certificates.first().encodeToDer(),
        signingAlgorithmOid = signatureAlgorithm.oid
    )

    val dtbsrResponse = client.post("${qtspHost}/sca/buildDtbs") {
        contentType(ContentType.Application.Json)
        setBody(vckJsonSerializer.encodeToString(dtbs))
    }
    val wrapper = vckJsonSerializer.decodeFromString<DtbsrWrapper>(dtbsrResponse.bodyAsText())

    return wrapper.transactionToken to OAuthDocumentDigest(wrapper.dtbsr, document.label)
}

//TODO probably moves into VCK when switching to internal processing
//For now use QTSP external SCA
internal suspend fun getFinishedDocuments(
    client: HttpClient,
    qtspHost: String,
    signatureResponse: QtspSignatureResponse,
    transactionTokens: List<String>,
    qtspIdentifier: String,
): List<FinishedDocument> {
    val finishedDocuments = mutableListOf<FinishedDocument>()

    when (signatureResponse) {
        is SignHashResponseParameters -> {
            require(signatureResponse.signatures != null) { "Signatures are null" }
            require(signatureResponse.signatures!!.size == transactionTokens.size) { "Signature to Transaction token mismatch!" }
            val signatures = signatureResponse.signatures!!.zip(transactionTokens)
                .map { (signatures, token) -> WrappedSignature(signatures, token) }

            for (wrappedSig in signatures) {
                val finishedDocResponse = client.post("${qtspHost}/sca/finalize") {
                    contentType(ContentType.Application.Json)
                    header("Wallet-QTSP-ID", qtspIdentifier) 
                    setBody(
                        vckJsonSerializer.encodeToString(
                            wrappedSig
                        )
                    )
                }
                val finishedDocument = vckJsonSerializer.decodeFromString<FinishedDocument>(finishedDocResponse.bodyAsText())
                finishedDocuments.add(finishedDocument)
            }
        }

        is SignDocResponseParameters -> TODO("Not in potential test scope")
    }
    return finishedDocuments
}