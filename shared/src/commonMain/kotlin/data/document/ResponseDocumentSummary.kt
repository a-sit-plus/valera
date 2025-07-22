package data.document

import at.asitplus.wallet.lib.agent.validation.CredentialFreshnessSummary
import at.asitplus.wallet.lib.data.IsoDocumentParsed
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.primitives.TokenStatusValidationResult

data class ResponseDocumentSummary(
    val docType: String,
    val freshnessSummary: CredentialFreshnessSummary.Mdoc,
    val validItems: List<String>,
    val invalidItems: List<String>
) {
    companion object {
        fun fromIsoDocumentParsed(isoDocumentParsed: IsoDocumentParsed): ResponseDocumentSummary =
            ResponseDocumentSummary(
                docType = isoDocumentParsed.mso.docType,
                freshnessSummary = isoDocumentParsed.freshnessSummary,
                validItems = isoDocumentParsed.validItems.map { it.elementIdentifier },
                invalidItems = isoDocumentParsed.invalidItems.map { it.elementIdentifier }
            )
    }
    val isTokenValid: Boolean
        get() = freshnessSummary.tokenStatusValidationResult is TokenStatusValidationResult.Valid

    val tokenStatusValidationResult: TokenStatusValidationResult
        get() = freshnessSummary.tokenStatusValidationResult

    val isMsoExpired: Boolean
        get() = freshnessSummary.timelinessValidationSummary.isExpired

    val isMsoNotYetValid: Boolean
        get() = freshnessSummary.timelinessValidationSummary.isNotYetValid

    val isMsoTimely: Boolean
        get() = freshnessSummary.timelinessValidationSummary.isTimely

    val isValid: Boolean
        get() = invalidItems.isEmpty() && isTokenValid && isMsoTimely

    override fun toString() : String = buildString {
        appendLine("ResponseDocumentSummary:")
        appendLine("  DocType: $docType")
        appendLine("  Token valid: $isTokenValid")
        appendLine(
            when {
                isMsoTimely -> "  MSO timely"
                isMsoExpired -> "  MSO expired"
                isMsoNotYetValid -> "  MSO not yet valid"
                else -> "  Invalid timelinessValidationSummary"
            }
        )
        appendLine("  Response document status: " +
                when (val result = tokenStatusValidationResult) {
                    is TokenStatusValidationResult.Valid ->
                        "Valid - Token Status = ${result.tokenStatus}"
                    is TokenStatusValidationResult.Invalid ->
                        "Invalid - Token Status = ${result.tokenStatus}"
                    is TokenStatusValidationResult.Rejected ->
                        "Rejected - Throwable:\n${result.throwable}"
                }
        )
        appendLine("  Valid items:")
        validItems.ifEmpty { listOf("(none)") }.forEach { appendLine("    - $it") }
        appendLine("  Invalid items:")
        invalidItems.ifEmpty { listOf("(none)") }.forEach { appendLine("    - $it") }
    }
}

fun getSummaryForDocType(
    summaries: List<ResponseDocumentSummary>,
    docType: String
): ResponseDocumentSummary? =
    summaries.firstOrNull { it.docType == docType }
