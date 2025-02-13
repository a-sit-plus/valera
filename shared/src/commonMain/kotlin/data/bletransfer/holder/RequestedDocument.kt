package data.bletransfer.holder

import data.bletransfer.verifier.DocumentAttributes

class RequestedDocument(
    val docType: String,
    var nameSpaces: List<NameSpace> = listOf()
) {
    fun addNameSpace(namespace: NameSpace?) {
        if (namespace != null) {
            nameSpaces += namespace
        }
    }

    override fun toString(): String {
        return buildString {
            appendLine("$docType: {")
            nameSpaces.forEach { namespace ->
                append("    ")
                appendLine(namespace.toString().trimStart())
            }
            append("}")
        }
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

        override fun toString(): String {
            return buildString {
                appendLine("$nameSpace: [")
                attributesMap.forEach { (key, value) ->
                    appendLine("    ${key.value} to $value,")
                }
                append("]")
            }
        }
    }
}
