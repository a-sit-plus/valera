import androidx.compose.runtime.MutableState
import io.github.aakira.napier.Napier

class ErrorService(var showError: MutableState<Boolean>, var errorText: MutableState<String>) {
    fun emit (e: Exception){
        if (e.message == null) {
            errorText.value = "Unknown Exception"
            Napier.e("Unknown Exception")
            Napier.e("StackTrace: " + e.stackTraceToString())
            showError.value = true
        } else {
            errorText.value = e.message.toString()
            Napier.e(e.message.toString())
            Napier.e("StackTrace: " + e.stackTraceToString())
            showError.value = true
        }
    }

    fun reset(){
        errorText.value = ""
        showError.value = false
    }
}