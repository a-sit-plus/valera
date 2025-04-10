package ui.viewmodels.intents

import at.asitplus.wallet.app.common.WalletMain
import io.ktor.http.parseQueryString

class ErrorIntentViewModel(
    val walletMain: WalletMain,
    val uri: String,
    val onFailure: (Throwable) -> Unit
) {
    fun process() {
        at.asitplus.catchingUnwrapped {
            val parameterIndex = uri.indexOfFirst { it == '?' }
            val pars = parseQueryString(uri, startIndex = parameterIndex + 1)
            throw(Exception(pars["error_description"] ?: "Unknown Exception"))
        }.onFailure {
            onFailure(it)
        }
    }
}