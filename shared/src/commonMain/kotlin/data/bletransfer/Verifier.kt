package data.bletransfer

import androidx.compose.runtime.Composable
import data.bletransfer.verifier.Entry

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

    data class Document(
        val docType: String,
        val requestDocument: Map<String, Map<String, Boolean>>
    )
}


