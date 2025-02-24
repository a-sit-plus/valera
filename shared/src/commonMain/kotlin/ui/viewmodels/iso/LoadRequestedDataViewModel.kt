package ui.viewmodels.iso

import data.bletransfer.getVerifier
import data.bletransfer.util.Document
import data.bletransfer.util.Entry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LoadRequestedDataViewModel(
    val document: Document,
    private val payload: String,
    val navigateUp: () -> Unit,
    val onError: (String) -> Unit
) {
    private val _entryState = MutableStateFlow<List<Entry>>(emptyList())
    val entryState = _entryState.asStateFlow()

    val verifier = getVerifier()

    private val updateData: (List<Entry>) -> Unit = { newEntries ->
        if(newEntries.isEmpty()) {
            onError("Response does not contain any document")
        }
        _entryState.value = newEntries
    }

    fun loadData() {
        verifier.verify(payload, document, updateData)
        verifier.disconnect()
    }
}
