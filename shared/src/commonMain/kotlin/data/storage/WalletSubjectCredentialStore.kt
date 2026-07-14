package data.storage

import at.asitplus.KmmResult
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.agent.SubjectCredentialStore.StoreEntry
import at.asitplus.wallet.lib.agent.Validator
import at.asitplus.wallet.lib.agent.validation.CredentialFreshnessSummary
import at.asitplus.wallet.lib.data.CredentialScheme
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.primitives.TokenStatusValidationResult
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface WalletSubjectCredentialStore : SubjectCredentialStore {
    val validator: Validator

    suspend fun reset()

    suspend fun removeStoreEntryById(storeEntryId: StoreEntryId)

    override suspend fun getCredentials(
        credentialSchemes: Collection<CredentialScheme>?,
    ): KmmResult<List<StoreEntry>> {
        val latestCredentials = observeStoreContainer().first().credentials.map { it.second }
        return KmmResult.success(
            credentialSchemes?.let { schemes ->
                latestCredentials.filter { it.resolveScheme() in schemes }
            } ?: latestCredentials,
        )
    }

    /**
     * Checks all stored credentials and returns those that are no longer fresh together with their
     * unique store entry IDs.
     */
    suspend fun getInvalidCredentials(): List<Pair<StoreEntryId, StoreEntry>> {
        val availableCredentials = observeStoreContainer().first().credentials
        if (availableCredentials.isEmpty()) return emptyList()

        return coroutineScope {
            availableCredentials.map { credential ->
                credential to async {
                    validator.checkCredentialFreshness(credential.second)
                }
            }.map { (credential, freshness) ->
                credential to freshness.await()
            }.filter { (credential, freshness) ->
                credential.second.renewalInfo != null && freshness.needsRefresh
            }.map { (credential, _) ->
                credential
            }
        }
    }

    fun observeStoreContainer(): Flow<StoreContainer>
}

private val CredentialFreshnessSummary.needsRefresh: Boolean
    get() = timelinessValidationSummary.isExpired ||
            tokenStatusValidationResult is TokenStatusValidationResult.Invalid
