package data.verifier.transfer.util

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.graphics.asImageBitmap
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.cbor.CBORFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import data.verifier.DocumentAttributes
import data.verifier.ValueType
import data.verifier.BooleanEntry
import data.verifier.Entry
import data.verifier.EntryValue
import data.verifier.ImageArray
import data.verifier.ImageEntry
import data.verifier.IntEntry
import data.verifier.ReceivedDocument
import data.verifier.StringEntry
import data.verifier.VehicleRegistration
import io.github.aakira.napier.Napier
import java.security.PrivateKey
class CborDecoder(
    var updateLogs: (String?, String) -> Unit = { _: String?, _: String -> }
) {
    private val TAG: String = "CborDecoder"
    private val cborFactory: CBORFactory = CBORFactory()
    private val objectMapper: ObjectMapper = ObjectMapper(cborFactory).apply {
        registerKotlinModule() // Register Kotlin module for better Kotlin compatibility
    }

    var entryList: List<Entry> = listOf()

    var documentRequests: List<ReceivedDocument> = listOf()


    private fun addEntry(cborData: ByteArray) {
        val identifier: String = cborMapExtractString(cborData, "elementIdentifier") ?: return
        val entryData: ValueType = DocumentAttributes.entries.associate { it.value to it.type }[identifier] ?: return

        val eval: EntryValue = when (entryData) {
            ValueType.DATE,
            ValueType.STRING -> StringEntry(cborMapExtractString(cborData, "elementValue") ?: "NOT FOUND")
            ValueType.INT -> IntEntry(cborMapExtractNumber(cborData, "elementValue") ?: 0)
            ValueType.IMAGE -> {
                val byteArray = cborMapExtractByteArray(cborData, "elementValue") ?: return
                val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)

                ImageEntry(bitmap.asImageBitmap())
            }
            ValueType.ARRAY -> {
                val list: List<Map<String, String>> = cborMapExtractArray(cborData, "elementValue") as? List<Map<String, String>> ?: return
                val vehicleList: MutableList<VehicleRegistration> = mutableListOf()

                for (item in list) {
                    val issueDate: String = item["issue_date"] ?: "SOMETHING WENT WRONG"
                    val expiryDate: String? = item["expiry_date"]
                    val vehicleCategory: String = item["vehicle_category_code"] ?: "SOMETHING WENT WRONG"
                    vehicleList += VehicleRegistration(issueDate, expiryDate, vehicleCategory)
                }

                ImageArray(vehicleList)
            }
            ValueType.BOOL -> BooleanEntry(cborMapExtractBoolean(cborData, "elementValue"))
            else -> {
                val decodedMap: Map<String, Any> = decodeCborData(cborData)
                StringEntry("For $identifier Not implemented yet ${decodedMap["elementValue"]!!::class.java}")
            }
        }
        entryList += Entry(identifier, DocumentAttributes.entries.associate { it.value to it.displayName }[identifier] ?: return, eval)
        entryList = entryList.sortedBy { entry ->
            DocumentAttributes.entries.indexOfFirst { it.value == entry.entryName }
        }
    }

    fun decodeResponse(encodedDeviceResponse: ByteArray,
                       sessionTranscript: ByteArray?,
                       ephemeralReaderKey: PrivateKey?
    ) {
        val documents: List<Map<String, Any>>? = cborMapExtractArray(encodedDeviceResponse, "documents")

        if (documents == null) {
            updateLogs(TAG, "No documents found!")
            return
        }

        updateLogs(TAG, "Found ${documents.size} documents")

        for (document in documents) {
            val docType = document["docType"] as? String
            Napier.d(tag = TAG, message = "docType: $docType")

            (document["issuerSigned"] as? Map<String, *>)?.let { issuerSigned ->
                (issuerSigned["nameSpaces"] as? Map<String, *>)?.let { namespaces ->
                    for (namespace: String in namespaces.keys) {
                        Napier.d(tag = TAG, message = "namespace: $namespace")
                        (namespaces[namespace] as? List<*>)?.let { entrys ->

                            for (entry in entrys) {
                                Napier.d(tag = TAG, message = "elementIdentifier: $entry")
                                if (entry != null) {
                                    val cbordata = entry as ByteArray
                                    Napier.d(tag = TAG, message = "identifier?: ${cborMapExtractString(cbordata, "elementIdentifier")}")
                                    addEntry(cbordata)
                                }

                            }
                        } ?: Napier.d(tag = TAG, message = "entrys is null")
                    }
                } ?: Napier.d(tag = TAG, message = "nameSpaces is null")
            } ?: Napier.d(tag = TAG, message = "issuer signed is null")
        }

        Napier.d(tag = TAG, message = "status: " + cborMapExtractNumber(encodedDeviceResponse, "status"))
    }

    fun decodeRequest(encodedDeviceRequest: ByteArray
    ) {
        updateLogs(TAG, "Decoding received cbor byte array")

        val docRequests: List<Map<String, Any>>? = cborMapExtractArray(encodedDeviceRequest, "docRequest")

        if (docRequests == null) {
            updateLogs(TAG, "No documents found!")
            return
        }

        updateLogs(TAG, "Found ${docRequests.size} docRequests")



        for (request in docRequests) {
            val items = request["itemsRequest"] as? ByteArray

            if (items != null) {
                val item: Map<String, *> = decodeCborData(items)
                val docType: String? = item["docType"] as? String
                val nameSpaces = item["nameSpaces"] as? Map<String, Map<String, *>>
                if (docType != null && nameSpaces != null) {
                    val document = ReceivedDocument(docType)
                    for (namespace in nameSpaces.keys) {
                        val nameSpaceObject: ReceivedDocument.NameSpace = ReceivedDocument.NameSpace(namespace)
                        for (attribute in nameSpaces[namespace]?.keys ?: emptyList()) {
                            nameSpaceObject.addAttribute(DocumentAttributes.fromValue(attribute))
                        }
                        document.addNameSpace(nameSpaceObject)
                    }


                    documentRequests += document
                }
            }
        }
    }

    private fun cborMapExtractString(cborData: ByteArray, key: String): String? =
        (decodeCborData(cborData) as Map<String, Any>)[key] as? String

    private fun cborMapExtractNumber(cborData: ByteArray, key: String): Number? =
        (decodeCborData(cborData) as Map<String, Any>)[key] as? Number

    private fun cborMapExtractArray(cborData: ByteArray, key: String): List<Map<String, Any>>? =
        (decodeCborData(cborData) as Map<String, Any>)[key] as? List<Map<String, Any>>

    private fun cborMapExtractByteArray(cborData: ByteArray, key: String): ByteArray? =
        (decodeCborData(cborData) as Map<String, Any>)[key] as? ByteArray

    private fun cborMapExtractBoolean(cborData: ByteArray, key: String): Boolean? =
        (decodeCborData(cborData) as Map<String, Any>)[key] as? Boolean

    private fun decodeCborData(cborData: ByteArray): Map<String, Any> =
        objectMapper.readValue(cborData, Map::class.java) as Map<String, Any>
}