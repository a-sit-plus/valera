package ui.viewmodels.iso

import data.bletransfer.Verifier
import data.bletransfer.getVerifier
import data.bletransfer.verifier.Entry
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LoadRequestedDataViewModel(
    val document: Verifier.Document,
    private val payload: String,
    val navigateUp: () -> Unit,
    val onError: (String) -> Unit
) {
    private val _entryState = MutableStateFlow<List<Entry>>(emptyList())
    val entryState = _entryState.asStateFlow()

    val verifier = getVerifier()

    private val updateLogs: (String?, String) -> Unit = { tag, message ->
        Napier.d("[$tag]: $message")
    }

    private val updateData: (List<Entry>) -> Unit = { newEntries ->
        if(newEntries.isEmpty()) {
            onError("Response does not contain documents")
        }
        _entryState.value = newEntries
    }

    fun loadData() {
        verifier.verify(payload, document, updateLogs, updateData)
        verifier.disconnect()
    }
}
