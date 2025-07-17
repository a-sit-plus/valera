package data.document

data class ResponseDocumentSummary(
    val docType: String,
    val isTokenValid: Boolean,
    val isMsoTimely: Boolean,
    val isValid: Boolean,
    val validItems: List<String>,
    val invalidItems: List<String>
) {
    override fun toString(): String = buildString {
        appendLine("ResponseDocumentSummary:")
        appendLine("  DocType: $docType")
        appendLine("  Token Valid: $isTokenValid")
        appendLine("  MSO Timely: $isMsoTimely")
        appendLine("  Valid: $isValid")
        appendLine("  Valid Items:")
        validItems.takeIf { it.isNotEmpty() }
            ?.forEach { appendLine("    - $it") }
            ?: appendLine("    (none)")
        appendLine("  Invalid Items:")
        invalidItems.takeIf { it.isNotEmpty() }
            ?.forEach { appendLine("    - $it") }
            ?: appendLine("    (none)")
    }
}
