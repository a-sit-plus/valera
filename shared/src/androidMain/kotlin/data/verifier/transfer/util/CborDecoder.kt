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
import data.verifier.StringEntry
import data.verifier.VehicleRegistration
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

    fun decode(encodedDeviceResponse: ByteArray,
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
            Log.d(TAG,"docType: $docType")
            val issuerSigned = document["issuerSigned"] as? Map<String, *>
            if (issuerSigned != null) {
                val namespaces = issuerSigned["nameSpaces"] as? Map<String, *>
                if (namespaces != null) {
                    for (namespace: String in namespaces.keys) {
                        Log.d(TAG,"namespace: $namespace")
                        val entrys = namespaces[namespace] as? List<*>

                        if (entrys != null) {
                            for (entry in entrys) {
                                Log.d(TAG,"elementIdentifier: $entry")
                                if (entry != null) {
                                    val cbordata = entry as ByteArray
                                    Log.d(TAG,"identifier?: ${cborMapExtractString(cbordata, "elementIdentifier")}")
                                    addEntry(cbordata)
                                }

                            }
                        } else {
                            Log.d(TAG,"entrys is null")
                        }
                    }
                    Log.d(TAG,"nameSpaces is: $namespaces")
                } else {
                    Log.d(TAG,"nameSpaces is null")
                }
            } else {
                Log.d(TAG,"issuer signed is null")
            }
        }

        Log.d(TAG,"status: " + cborMapExtractNumber(encodedDeviceResponse, "status"))
    }



    private fun cborMapExtractString(cborData: ByteArray, key: String): String? {
        val decodedMap: Map<String, Any> = decodeCborData(cborData)
        return decodedMap[key] as? String
    }

    private fun cborMapExtractNumber(cborData: ByteArray, key: String): Number? {
        val decodedMap: Map<String, Any> = decodeCborData(cborData)
        return decodedMap[key] as? Number
    }

    private fun cborMapExtractArray(cborData: ByteArray, key: String): List<Map<String, Any>>? {
        val decodedMap: Map<String, Any> = decodeCborData(cborData)
        return decodedMap[key] as? List<Map<String, Any>>
    }

    private fun cborMapExtractByteArray(cborData: ByteArray, key: String): ByteArray? {
        val decodedMap: Map<String, Any> = decodeCborData(cborData)
        return decodedMap[key] as? ByteArray
    }

    private fun cborMapExtractBoolean(cborData: ByteArray, key: String): Boolean? {
        val decodedMap: Map<String, Any> = decodeCborData(cborData)
        return decodedMap[key] as? Boolean
    }

    private fun decodeCborData(cborData: ByteArray): Map<String, Any> {
        return objectMapper.readValue(cborData, Map::class.java) as Map<String, Any>
    }
}