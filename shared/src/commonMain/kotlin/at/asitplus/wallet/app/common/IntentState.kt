package at.asitplus.wallet.app.common

import at.asitplus.wallet.app.common.dcapi.DCAPIInvocationData
import kotlinx.coroutines.flow.MutableStateFlow
import ui.viewmodels.authentication.PresentationStateModel

/**
 * Shared in-memory state used to bridge platform intents/sessions into common UI navigation.
 *
 * A single instance is typically held by the platform entrypoint (Android/iOS) and passed into
 * [ui.navigation.WalletNavigation] to coordinate deep links, DCAPI request data, and callback behaviour.
 */
class IntentState {
    /** Pending app/deep link to be processed by navigation. */
    val appLink = MutableStateFlow<String?>(null)

    /** Active Digital Credentials API invocation/session payload, if any. */
    val dcapiInvocationData = MutableStateFlow<DCAPIInvocationData?>(null)

    /** Presentation model passed between routes during local presentment flows. */
    val presentationStateModel = MutableStateFlow<PresentationStateModel?>(null)

    /**
     * Optional platform callback for "finish/return to caller".
     *
     * Platforms that cannot or should not close the host app can leave this `null`.
     * Navigation code must handle `null` by falling back to local navigation.
     */
    var finishApp: (() -> Unit)? = null
}
