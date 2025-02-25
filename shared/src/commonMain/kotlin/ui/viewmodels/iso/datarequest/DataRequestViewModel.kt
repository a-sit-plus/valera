package ui.viewmodels.iso.datarequest

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import at.asitplus.wallet.app.common.WalletMain
import io.github.aakira.napier.Napier

class DataRequestViewModel(
    val navigateUp: () -> Unit,
    val walletMain: WalletMain,
) {
    val attributes = walletMain.holder.getAttributes()
    var viewState by mutableStateOf(DataRequestViewState.Consent)

    fun onConsent() {
        // TODO: find matching credentials and present it to the user
        Napier.d("HandleRequestedDataViewModel: onConsent()")
        viewState = DataRequestViewState.Selection
    }

}

enum class DataRequestViewState {
    Consent,
    NoMatchingCredential,
    Loading,
    Selection,
    Sent
}
