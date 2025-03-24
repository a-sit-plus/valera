package data.bletransfer.util

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.asImageBitmap
import at.asitplus.signum.indispensable.cosef.CoseSigned
import at.asitplus.signum.indispensable.cosef.io.ByteStringWrapper
import at.asitplus.wallet.lib.iso.DeviceRequest
import at.asitplus.wallet.lib.iso.DeviceResponse
import at.asitplus.wallet.lib.iso.IssuerSignedItem
import at.asitplus.wallet.mdl.DrivingPrivilege
import com.android.identity.crypto.EcPrivateKey
import com.android.identity.crypto.javaX509Certificate
import data.bletransfer.verifier.IdentityVerifier
import io.github.aakira.napier.Napier
import kotlinx.datetime.LocalDate
import java.security.cert.X509Certificate

class CborDecoder {
    private val TAG: String = "CborDecoder"

    var entryList = mutableListOf<Entry>()
    var documentRequests: List<RequestedDocument> = emptyList()
    var requesterIdentity: String? = null

    fun decodeResponse(
        encodedDeviceResponse: ByteArray,
        sessionTranscript: ByteArray?,
        ephemeralReaderKey: EcPrivateKey?
    ) {
        val deviceResponse: DeviceResponse =
            DeviceResponse.deserialize(encodedDeviceResponse).getOrElse { exception ->
                val errorMessage = "Deserialization of DeviceResponse failed"
                Napier.e(TAG, exception, errorMessage)
                return
            }


        Napier.i(tag = TAG, message = "DeviceResponse: status=${deviceResponse.status}, version=${deviceResponse.version}")
        if (deviceResponse.documentErrors?.isNotEmpty() == true) {
            Napier.e(tag = TAG, message = "Document contains errors: ${deviceResponse.documentErrors}")
            return
        }

        val documents = deviceResponse.documents
        if (documents.isNullOrEmpty()) {
            Napier.e(tag = TAG, message = "No document found in DeviceResponse")
            return
        }

        for (document in documents) {
            document.issuerSigned.namespaces?.forEach { (_, issuerSignedList) ->
                issuerSignedList.entries.forEach { entry ->
                    parseEntry(entry)?.let {
                        entryList.add(it)
                    } ?: Napier.e(tag = TAG, message = "Failed to parse entry: $entry")
                }
            }
        }
    }

    private fun parseEntry(issuerSignedEntry: ByteStringWrapper<IssuerSignedItem>) : Entry? {
        val issuerSignedItem = issuerSignedEntry.value
        val elementIdentifier = issuerSignedItem.elementIdentifier
        val elementValue = issuerSignedItem.elementValue
        val valueType = DocumentAttributes.entries.associate { it.value to it.type }[elementIdentifier] ?: return null

        val entryValue: EntryValue = when (valueType) {
            ValueType.STRING -> StringEntry(elementValue as? String ?: "")

            ValueType.IMAGE -> {
                val base64String = elementValue as? String ?: return null
                val byteArray = try {
                    Base64.decode(base64String, Base64.DEFAULT)
                } catch (e: IllegalArgumentException) {
                    Napier.e(tag = TAG, message = "Invalid Base64 image data")
                    return null
                }
                val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                ImageEntry(bitmap?.asImageBitmap() ?: return null)
            }

            ValueType.BOOL -> BooleanEntry(elementValue as? Boolean ?: false)

            ValueType.DATE -> StringEntry((elementValue as? LocalDate)?.toString().orEmpty())

            ValueType.INT -> IntEntry((elementValue as? Number) ?: 0)

            ValueType.DRIVING_PRIVILEGES -> DrivingPrivilegesEntry(elementValue as? Array<DrivingPrivilege> ?: return null)
        }

        val entryDisplayName = DocumentAttributes.entries.associate { it.value to it.displayName }[elementIdentifier] ?: return null
        return Entry(entryDisplayName, entryValue)
    }

    fun decodeRequest(encodedDeviceRequest: ByteArray, sessionTranscript: ByteArray?, context: Context) {
        val deviceRequest = DeviceRequest.deserialize(encodedDeviceRequest).getOrElse {
            Napier.e(tag = TAG, throwable = it, message = "Deserialization of DeviceRequest failed")
            return
        }

        val docRequests = deviceRequest.docRequests
        Napier.w(tag = TAG, message =  docRequests[0].toString())

        if (docRequests.isEmpty()) {
            Napier.w(tag = TAG, message =  "No document found in DeviceRequest")
            return
        }



//        documentRequests += docRequests.map { request ->
//            RequestedDocument(request.itemsRequest.value.docType).apply {
//                request.itemsRequest.value.namespaces.forEach { (namespaceKey, namespaceValue) ->
//                    addNameSpace(RequestedDocument.NameSpace(namespaceKey).apply {
//                        namespaceValue.entries.forEach { (attributeKey, value) ->
//                            addAttribute(DocumentAttributes.fromValue(attributeKey))
//                        }
//                    })
//                }
//            }
//        }

        val requestsAndAuth: Map<RequestedDocument, CoseSigned<ByteArray>?> = docRequests.associate { request ->
            val requestedDocument = RequestedDocument(request.itemsRequest.value.docType).apply {
                request.itemsRequest.value.namespaces.forEach { (namespaceKey, namespaceValue) ->
                    addNameSpace(RequestedDocument.NameSpace(namespaceKey).apply {
                        namespaceValue.entries.forEach { (attributeKey, value) ->
                            addAttribute(DocumentAttributes.fromValue(attributeKey), value)
                        }
                    })
                }
            }
            requestedDocument to request.readerAuth
        }
        for ((key, value) in requestsAndAuth) {
            if (value != null) {
                if (!IdentityVerifier.verifyReaderIdentity(key, value, sessionTranscript, context)) {
                    return
                }
            }
        }
        documentRequests = requestsAndAuth.keys.toList()
        requesterIdentity = IdentityVerifier.requesterIdentity

    }
}
