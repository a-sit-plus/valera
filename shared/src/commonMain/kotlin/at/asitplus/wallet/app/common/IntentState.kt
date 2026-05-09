package at.asitplus.wallet.app.common

import at.asitplus.wallet.app.common.dcapi.DCAPIInvocationData
import at.asitplus.wallet.app.dcapi.IosDcApiPreRequestData
import kotlinx.atomicfu.atomic
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

    /** Pending iOS-only Digital Credentials API pre-request payload, if any. */
    val iosDcApiPreRequestData = MutableStateFlow<IosDcApiPreRequestData?>(null)

    /** Presentation model passed between routes during local presentment flows. */
    val presentationStateModel = MutableStateFlow<PresentationStateModel?>(null)

    /**
     * Optional platform callback for "finish/return to caller".
     *
     * Platforms that cannot or should not close the host app can leave this `null`.
     * Navigation code must handle `null` by falling back to local navigation.
     *
     * Backed by an [atomic] ref: on iOS this field is written under
     * `IosSessionRuntime.stateLock` from the extension/Swift thread, but read from
     * the UI (Main) thread in navigation code. A plain `var` has undefined visibility
     * across threads on Kotlin/Native's strict memory model.
     */
    private val _finishApp = atomic<(() -> Unit)?>(null)
    var finishApp: (() -> Unit)?
        get() = _finishApp.value
        set(value) { _finishApp.value = value }

    /** Clears all transient navigation state. Call before a session reset so navigation recomposes cleanly. */
    fun reset() {
        appLink.value = null
        dcapiInvocationData.value = null
        iosDcApiPreRequestData.value = null
        presentationStateModel.value = null
    }
}
