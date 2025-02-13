package ui.viewmodels.iso

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import data.bletransfer.Verifier
import data.bletransfer.getVerifier
import data.bletransfer.verifier.Entry
import io.github.aakira.napier.Napier

class LoadRequestedDataViewModel(
    val document: Verifier.Document,
    val payload: String,
    val navigateUp: () -> Unit,
    val onError: (String) -> Unit
) {
    val entryState: MutableState<List<Entry>> = mutableStateOf(emptyList())
    val verifier = getVerifier()

    val updateLogs: (String?, String) -> Unit = { tag, message ->
        Napier.d("[$tag]: $message")
    }

    val updateData: (List<Entry>) -> Unit = { newEntries ->
        if(newEntries.isEmpty()) {
            onError("Response does not contain documents")
        }
        entryState.value = newEntries
    }

    fun loadData() {
        verifier.verify(payload, document, updateLogs, updateData)
    }
}
