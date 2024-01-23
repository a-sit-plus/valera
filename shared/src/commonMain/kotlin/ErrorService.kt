import androidx.compose.runtime.MutableState
import io.github.aakira.napier.Napier

class ErrorService(var showError: MutableState<Boolean>, var errorText: MutableState<String>) {
    fun emit (e: Throwable){
        errorText.value = e.message ?: "Unknown exception"
        showError.value = true
        Napier.e("Error", e)
    }

    fun reset(){
        errorText.value = ""
        showError.value = false
    }
}