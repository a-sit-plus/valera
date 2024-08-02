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
        var attributes: List<DocumentAttributes> = listOf()
    ) {
        fun addAttribute(attribute: DocumentAttributes?) {
            if (attribute != null) {
                attributes += attribute
            }
        }
        fun log() {
            Napier.d("$nameSpace: [", tag="myTag")
            for (attribute in attributes) {
                Napier.d("${attribute.value},", tag="myTag")
            }
            Napier.d("]", tag="myTag")
        }
    }
}
