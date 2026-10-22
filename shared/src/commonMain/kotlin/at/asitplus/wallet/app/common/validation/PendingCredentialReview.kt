package at.asitplus.wallet.app.common.validation

import at.asitplus.wallet.lib.agent.PreparedCredentialStorage
import at.asitplus.wallet.lib.data.CredentialScheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update

/**
 * Issued credentials that have been rejected under [ValidationPolicies.issuance] for reasons the user may tolerate,
 * e.g. an untrusted issuer or claims the issuer left out, which are kept in memory, never persisted, until the user
 * decides to store or discard them. Session-scoped, so that they do not outlive the session they have been issued in.
 * The credentials of an issuance are reviewed only once each of them has been validated, so that one failing its
 * integrity leaves none of the others in a later review, and replace those of an earlier review the user left
 * without deciding.
 */
class PendingCredentialReview {
    /**
     * A [prepared] credential with its validation and renewal info, to store it after the user's consent, the [scheme]
     * the wallet requested it as, and the [outcome] of its validation to show.
     */
    data class Entry(
        val prepared: PreparedCredentialStorage,
        val scheme: CredentialScheme,
        val outcome: ValidationOutcomeUi,
    )

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries.asStateFlow()

    /** Replaces every entry with [entries], i.e. the credentials of the latest issuance to review. */
    fun replace(entries: List<Entry>) {
        _entries.value = entries.toList()
    }

    /** Removes and returns every entry, e.g. to store them once the user decided to. */
    fun take(): List<Entry> = _entries.getAndUpdate { emptyList() }

    fun clear() = _entries.update { emptyList() }
}
