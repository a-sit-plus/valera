package data.verifier

import androidx.compose.runtime.Composable
import kotlinx.serialization.Serializable

expect fun getVerifier(): Verifier

interface Verifier {
    @Composable
    fun getRequirements(check: (Boolean) -> Unit)

    fun verify(
        qrcode: String,
        requestedDocument: Document,
        updateLogs: (String?, String) -> Unit,
        updateData: (List<Entry>) -> Unit
    )

    fun disconnect()

    @Serializable
    data class Document(
        val docType: String,
        val requestDocument: Map<String, Map<String, Boolean>>
    )
}


