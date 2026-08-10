package at.asitplus.wallet.app.android.dcapi

import at.asitplus.wallet.app.common.dcapi.DCAPICredentialRepresentation
import at.asitplus.wallet.app.common.dcapi.DCAPICredentialType
import at.asitplus.wallet.app.common.dcapi.DCAPIExportService

internal class DCAPIVerificationSelection(
    val protocol: String,
    documentIds: List<String>,
) {
    private val selectedDocuments = documentIds.map { documentId ->
        SelectedDocument(documentId, SyntheticCredentialId.decode(documentId))
    }.toMutableList()

    fun nextIssuanceType(): DCAPICredentialType? =
        selectedDocuments.firstNotNullOfOrNull { it.syntheticType }

    fun resolve(type: DCAPICredentialType, credentialId: String) {
        require(credentialId.isNotBlank()) { "Issued credential has no DC API ID" }
        require(nextIssuanceType() == type) { "Issued credential type does not match the pending request" }
        selectedDocuments.replaceAll { selected ->
            if (selected.syntheticType == type) SelectedDocument(credentialId, null) else selected
        }
    }

    fun resolvedDocumentIds(): List<String> {
        check(nextIssuanceType() == null) { "Verification still requires credential issuance" }
        return selectedDocuments.map { it.documentId }
    }

    private data class SelectedDocument(
        val documentId: String,
        val syntheticType: DCAPICredentialType?,
    )
}

internal object SyntheticCredentialId {
    private const val MDOC_TOKEN = "mdoc"
    private const val SD_JWT_TOKEN = "sdjwt"
    private val prefix = "${DCAPIExportService.ISSUING_CREDENTIAL_ID}:"

    fun decode(documentId: String): DCAPICredentialType? {
        if (!documentId.startsWith(prefix)) return null
        val parts = documentId.removePrefix(prefix).split(':', limit = 2)
        require(parts.size == 2 && parts[1].isNotEmpty()) { "Malformed issuing credential ID" }
        val representation = when (parts[0]) {
            MDOC_TOKEN -> DCAPICredentialRepresentation.ISO_MDOC
            SD_JWT_TOKEN -> DCAPICredentialRepresentation.SD_JWT
            else -> throw IllegalArgumentException("Unknown issuing credential representation")
        }
        return DCAPICredentialType(representation, decodeHex(parts[1]))
    }

    private fun decodeHex(value: String): String {
        require(value.length % 2 == 0 && value.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
            "Malformed issuing credential type"
        }
        return ByteArray(value.length / 2) { index ->
            value.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }.decodeToString(throwOnInvalidSequence = true)
    }
}
