import androidx.compose.runtime.MutableState

class ErrorService(var showError: MutableState<Boolean>, var errorText: MutableState<String>) {
    fun emit (e: Exception){
        if (e.message == null) {
            errorText.value = "Unknown Exception"
            showError.value = true
        } else {
            errorText.value = e.message.toString()
            showError.value = true
        }
    }

    fun reset(){
        errorText.value = ""
        showError.value = false
    }
}