package at.asitplus.wallet.app.common

import io.github.aakira.napier.Napier

class ErrorService(val showError: (message: String?, cause: String?) -> Unit) {
    fun emit (e: Throwable){
        showError(e.message, e.cause?.message)
        Napier.e("Error", e)
    }
}