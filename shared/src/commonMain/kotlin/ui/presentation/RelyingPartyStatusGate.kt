package ui.presentation

import at.asitplus.wallet.app.common.relyingParty.RelyingPartyStatusResolution
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.getAndUpdate

/**
 * Holds back a presentation the user confirmed while the status of the registration certificates of the current
 * [relyingParty] is still being retrieved: [isAwaitingStatus] asks the user, with [PendingRelyingPartyStatusDialog],
 * to [waitForStatus], or to [sendWithoutStatus]. Once the status arrives, the question is dropped, so that the user
 * decides again with the updated validation of the relying party.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RelyingPartyStatusGate<T>(
    private val relyingParty: StateFlow<RelyingPartyStatusResolution?>,
    private val submit: (T) -> Unit,
) {
    private val awaitingStatus = MutableStateFlow<T?>(null)

    private val isStatusPending: Boolean
        get() = relyingParty.value?.isStatusPending?.value == true

    /** Whether to ask the user to wait for the status, or to send without it. */
    val isAwaitingStatus: Flow<Boolean> = combine(
        awaitingStatus,
        relyingParty.flatMapLatest { it?.isStatusPending ?: flowOf(false) },
    ) { awaiting, pending -> awaiting != null && pending }

    /** Submits [submission], or holds it back while the status is pending. */
    fun confirm(submission: T) {
        awaitingStatus.value = null
        if (isStatusPending) awaitingStatus.value = submission
        else submit(submission)
    }

    /** Submits the presentation held back, without the status of the relying party. */
    fun sendWithoutStatus() {
        awaitingStatus.getAndUpdate { null }?.let(submit)
    }

    /** Drops the presentation held back, to wait for the status of the relying party. */
    fun waitForStatus() {
        awaitingStatus.value = null
    }
}
