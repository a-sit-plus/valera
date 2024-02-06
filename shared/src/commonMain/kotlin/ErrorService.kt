import androidx.compose.runtime.MutableState
import io.github.aakira.napier.Napier

class ErrorService(var showError: MutableState<Boolean>, var throwable: MutableState<Throwable?>) {
    fun emit (e: Throwable){
        throwable.value = e
        showError.value = true
        Napier.e("Error", e)
    }

    fun reset(){
        throwable.value = null
        showError.value = false
    }
}