package at.asitplus.wallet.app.common.validation

import at.asitplus.wallet.lib.agent.CredentialRenewalInfo
import at.asitplus.wallet.lib.agent.Holder
import at.asitplus.wallet.lib.data.CredentialScheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update

/**
 * Issued credentials that have been rejected under [ValidationPolicies.strict] for reasons the user may tolerate, e.g.
 * an untrusted issuer, which are kept in memory, never persisted, until the user decides to store or discard them.
 * Session-scoped, so that they do not outlive the session they have been issued in.
 */
class PendingCredentialReview {
    /** A [credential] with its [renewalInfo], to store it as it would have been, and the [outcome] to show. */
    data class Entry(
        val credential: Holder.StoreCredentialInput,
        val renewalInfo: CredentialRenewalInfo?,
        val outcome: ValidationOutcomeUi,
    ) {
        val scheme: CredentialScheme
            get() = when (credential) {
                is Holder.StoreCredentialInput.Vc -> credential.scheme
                is Holder.StoreCredentialInput.SdJwt -> credential.scheme
                is Holder.StoreCredentialInput.Iso -> credential.scheme
            }
    }

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries.asStateFlow()

    fun add(entry: Entry) = _entries.update { it + entry }

    /** Removes and returns every entry, e.g. to store them once the user decided to. */
    fun take(): List<Entry> = _entries.getAndUpdate { emptyList() }

    fun clear() = _entries.update { emptyList() }
}
