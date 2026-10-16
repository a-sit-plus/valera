package at.asitplus.wallet.app.common.validation

import at.asitplus.wallet.lib.agent.Holder
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
 */
class PendingCredentialReview {
    /**
     * A [prepared] credential with its validation and renewal info, to store it after the user's consent, and the
     * [outcome] of its validation to show.
     */
    data class Entry(
        val prepared: PreparedCredentialStorage,
        val outcome: ValidationOutcomeUi,
    ) {
        val scheme: CredentialScheme
            get() = prepared.credential.scheme
    }

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries.asStateFlow()

    fun add(entry: Entry) = _entries.update { it + entry }

    /** Removes and returns every entry, e.g. to store them once the user decided to. */
    fun take(): List<Entry> = _entries.getAndUpdate { emptyList() }

    fun clear() = _entries.update { emptyList() }
}

/** The scheme the wallet requested this credential as. */
val Holder.StoreCredentialInput.scheme: CredentialScheme
    get() = when (this) {
        is Holder.StoreCredentialInput.Vc -> scheme
        is Holder.StoreCredentialInput.SdJwt -> scheme
        is Holder.StoreCredentialInput.Iso -> scheme
    }
