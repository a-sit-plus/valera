package data.bletransfer

import androidx.compose.runtime.Composable
import data.bletransfer.util.Document
import data.bletransfer.util.Entry

expect fun getVerifier(): Verifier

interface Verifier {
    @Composable
    fun getRequirements()

    fun verify(
        qrcode: String,
        requestedDocument: Document,
        updateData: (List<Entry>) -> Unit
    )

    fun disconnect()
}
