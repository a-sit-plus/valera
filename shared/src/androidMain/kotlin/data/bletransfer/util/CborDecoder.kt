package data.bletransfer.util

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.asImageBitmap
import at.asitplus.signum.indispensable.cosef.io.ByteStringWrapper
import at.asitplus.wallet.lib.iso.DeviceResponse
import at.asitplus.wallet.lib.iso.IssuerSignedItem
import at.asitplus.wallet.mdl.DrivingPrivilege
import com.android.identity.crypto.EcPrivateKey
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.cbor.CBORFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import data.bletransfer.holder.RequestedDocument
import data.bletransfer.verifier.BooleanEntry
import data.bletransfer.verifier.DocumentAttributes
import data.bletransfer.verifier.Entry
import data.bletransfer.verifier.EntryValue
import data.bletransfer.verifier.DrivingPrivilegesEntry
import data.bletransfer.verifier.ImageEntry
import data.bletransfer.verifier.IntEntry
import data.bletransfer.verifier.StringEntry
import data.bletransfer.verifier.ValueType
import io.github.aakira.napier.Napier
import kotlinx.datetime.LocalDate

class CborDecoder(
    var updateLogs: (String?, String) -> Unit = { _: String?, _: String -> }
) {
    private val TAG: String = "CborDecoder"
    private val objectMapper: ObjectMapper = ObjectMapper(CBORFactory()).apply {
        registerKotlinModule()
    }

    var entryList = mutableListOf<Entry>()
    var documentRequests: List<RequestedDocument> = emptyList()

    fun decodeResponse(
        encodedDeviceResponse: ByteArray,
        sessionTranscript: ByteArray?,
        ephemeralReaderKey: EcPrivateKey?
    ) {
        val deviceResponse: DeviceResponse =
            DeviceResponse.deserialize(encodedDeviceResponse).getOrElse { exception ->
                val errorMessage = "decodeResponse: deserialization of DeviceResponse failed"
                Napier.e(TAG, exception, errorMessage)
                updateLogs(TAG, errorMessage)
                return
            }

        Napier.i(tag = TAG, message = "deviceResponse: status=${deviceResponse.status}")
        Napier.i(tag = TAG, message = "deviceResponse: version=${deviceResponse.version}")

        if (deviceResponse.documentErrors?.isNotEmpty() == true) {
            Napier.e(tag = TAG, message = "Document contains errors: ${deviceResponse.documentErrors}")
            return
        }

        val documents = deviceResponse.documents
        if (documents.isNullOrEmpty()) {
            updateLogs(TAG, "No document found!")
            Napier.e(tag = TAG, message = "No document found in device response")
            return
        }

        for (document in documents) {
            Napier.d(tag = TAG, message = "Processing docType: ${document.docType}")
            document.issuerSigned.namespaces?.forEach { (namespace, issuerSignedList) ->
                Napier.d(tag = TAG, message = "Namespace: $namespace")
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
            ValueType.DATE -> StringEntry((elementValue as? LocalDate)?.toString().orEmpty())

            ValueType.STRING -> StringEntry(elementValue as? String ?: "")

            ValueType.INT -> IntEntry((elementValue as? Number) ?: 0)

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

            ValueType.DRIVING_PRIVILEGES -> {
                DrivingPrivilegesEntry(elementValue as? Array<DrivingPrivilege> ?: return null)
            }

            ValueType.BOOL -> BooleanEntry(elementValue as? Boolean ?: false)
        }

        val entryDisplayName = DocumentAttributes.entries.associate { it.value to it.displayName }[elementIdentifier] ?: return null
        return Entry(entryDisplayName, entryValue)
    }

    fun decodeRequest(encodedDeviceRequest: ByteArray) {
        updateLogs(TAG, "Decoding received cbor byte array")

        val docRequests: List<Map<String, Any>>? = cborMapExtractArray(encodedDeviceRequest, "docRequests")
        if (docRequests.isNullOrEmpty()) {
            updateLogs(TAG, "No docRequest found!")
            return
        }

        updateLogs(TAG, "Found ${docRequests.size} docRequests")
        for (request in docRequests) {
            (request["itemsRequest"] as? ByteArray)?.let { items ->
                val item: Map<String, *> = decodeCborData(items)
                (item["docType"] as? String)?.let { docType ->
                    val document = RequestedDocument(docType)
                    (item["nameSpaces"] as? Map<String, Map<String, *>>)?.let { nameSpaces ->
                        for (namespace in nameSpaces.keys) {
                            val nameSpaceObject = RequestedDocument.NameSpace(namespace)
                            for (attribute in nameSpaces[namespace]?.keys ?: emptyList()) {
                                nameSpaceObject.addAttribute(DocumentAttributes.fromValue(attribute))
                            }
                            document.addNameSpace(nameSpaceObject)
                        }
                    } ?: Napier.d(tag = TAG, message = "nameSpaces is null")
                    documentRequests += document
                } ?: Napier.d(tag = TAG, message = "docType is null")
            } ?: Napier.d(tag = TAG, message = "itemsRequest is null")
        }
    }

    private fun cborMapExtractArray(cborData: ByteArray, key: String): List<Map<String, Any>>? {
        val decodedMap = decodeCborData(cborData)
        val list = decodedMap[key] as? List<*>
        return list?.filterIsInstance<Map<String, Any>>()?.takeIf { it.size == list.size }
    }

    private fun decodeCborData(cborData: ByteArray): Map<String, Any> =
        objectMapper.readValue(cborData, object : TypeReference<Map<String, Any>>() {})
}
