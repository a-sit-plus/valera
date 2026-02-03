package data.storage

import at.asitplus.KmmResult
import at.asitplus.iso.IssuerSigned
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.SelectiveDisclosureItem
import at.asitplus.wallet.lib.data.VerifiableCredentialJws
import at.asitplus.wallet.lib.data.VerifiableCredentialSdJwt
import at.asitplus.wallet.lib.ktor.openid.RefreshTokenInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn

/**
 * This class is used in order to reduce the time needed to load credentials from the store in HolderAgent
 * TODO: Evaluate, whether this takes too much memory or if the performance improvements are worth it
 */
class HotWalletSubjectCredentialStore(
    private val delegate: PersistentSubjectCredentialStore,
    val coroutineScope: CoroutineScope,
) : WalletSubjectCredentialStore, SubjectCredentialStore {
    override suspend fun reset() = delegate.reset()

    val hotStoreContainer: StateFlow<StoreContainer?> = delegate.observeStoreContainer().stateIn(
        scope = coroutineScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    override fun observeStoreContainer(): Flow<StoreContainer> = hotStoreContainer.filterNotNull()

    override suspend fun getCredentials(credentialSchemes: Collection<ConstantIndex.CredentialScheme>?): KmmResult<List<SubjectCredentialStore.StoreEntry>> {
        val latestCredentials = observeStoreContainer().first().credentials.map { it.second }
        return credentialSchemes?.let { schemes ->
            KmmResult.success(latestCredentials.filter {
                when (it) {
                    is SubjectCredentialStore.StoreEntry.Iso -> it.scheme in schemes
                    is SubjectCredentialStore.StoreEntry.SdJwt -> it.scheme in schemes
                    is SubjectCredentialStore.StoreEntry.Vc -> it.scheme in schemes
                }
            }.toList())
        } ?: KmmResult.success(latestCredentials)
    }

    override suspend fun getInvalidCredentials(): List<Pair<StoreEntryId, SubjectCredentialStore.StoreEntry>> = delegate.getInvalidCredentials()

    override suspend fun removeStoreEntryById(
        storeEntryId: StoreEntryId,
    ) = delegate.removeStoreEntryById(storeEntryId)

    override suspend fun storeCredential(
        vc: VerifiableCredentialJws,
        vcSerialized: String,
        scheme: ConstantIndex.CredentialScheme,
        refreshToken: RefreshTokenInfo?
    ): SubjectCredentialStore.StoreEntry = delegate.storeCredential(
        vc = vc,
        vcSerialized = vcSerialized,
        scheme = scheme,
        refreshToken = refreshToken,
    )

    override suspend fun storeCredential(
        vc: VerifiableCredentialSdJwt,
        vcSerialized: String,
        disclosures: Map<String, SelectiveDisclosureItem?>,
        scheme: ConstantIndex.CredentialScheme,
        refreshToken: RefreshTokenInfo?
    ): SubjectCredentialStore.StoreEntry = delegate.storeCredential(
        vc = vc,
        vcSerialized = vcSerialized,
        disclosures = disclosures,
        scheme = scheme,
        refreshToken
    )

    override suspend fun storeCredential(
        issuerSigned: IssuerSigned,
        scheme: ConstantIndex.CredentialScheme,
        refreshToken: RefreshTokenInfo?
    ): SubjectCredentialStore.StoreEntry = delegate.storeCredential(
        issuerSigned = issuerSigned,
        scheme = scheme,
        refreshToken = refreshToken
    )
}