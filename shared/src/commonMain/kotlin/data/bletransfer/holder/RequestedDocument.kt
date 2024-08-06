package data.bletransfer.holder

import data.bletransfer.verifier.DocumentAttributes
import io.github.aakira.napier.Napier

class RequestedDocument(
    val docType: String,
    var nameSpaces: List<NameSpace> = listOf()
) {

    fun addNameSpace(namespace: NameSpace?) {
        if (namespace != null) {
            nameSpaces += namespace
        }
    }

    fun log() {
        Napier.d("$docType: {", tag="myTag")
        for (namespace in nameSpaces) {
            namespace.log()
        }
        Napier.d("}", tag="myTag")
    }

    class NameSpace(
        val nameSpace: String,
        var attributesMap: MutableMap<DocumentAttributes, Boolean> = mutableMapOf()
    ) {
        val attributes: List<DocumentAttributes>
            get() = attributesMap.keys.toList()

        val trueAttributes: List<DocumentAttributes>
            get() = attributesMap.filter { it.value }.keys.toList()

        fun addAttribute(attribute: DocumentAttributes?) {
            if (attribute != null) {
                attributesMap[attribute] = false
            }
        }
        fun log() {
            Napier.d("$nameSpace: [", tag="myTag")
            for (attribute in attributesMap) {
                Napier.d("${attribute.key.value} to ${attribute.value},", tag="myTag")
            }
            Napier.d("]", tag="myTag")
        }
    }
}

/*class ResponseDocument(
    val namespace: String,
    val entry: Entry
) {

    class Entry(
        val entryType: DocumentAttributes
    ) {

        class EString(val item: String){

        }: Entry
        class EImage(val item: String){

        }: Entry
    }
}*/

