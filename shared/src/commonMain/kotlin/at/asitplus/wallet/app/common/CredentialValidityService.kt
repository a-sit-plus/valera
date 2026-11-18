package at.asitplus.wallet.app.common

import at.asitplus.catchingUnwrapped
import at.asitplus.data.NonEmptyList.Companion.nonEmptyListOf
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.error_no_refresh_token
import at.asitplus.valera.resources.error_reissue_failed
import at.asitplus.valera.resources.error_reissue_in_progress
import at.asitplus.valera.resources.success_refreshed
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.uiLabelNonCompose
import at.asitplus.wallet.app.common.validation.StoredCredentialValidator
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.CredentialScheme
import at.asitplus.wallet.lib.oidvci.CredentialIdentifierInfo
import data.storage.DataStoreService
import data.storage.StoreEntryId
import data.storage.WalletSubjectCredentialStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.getString
import kotlin.concurrent.Volatile
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class CredentialValidityService(
    private val subjectCredentialStore: WalletSubjectCredentialStore,
    private val snackbarService: SnackbarService,
    private val provisioningService: ProvisioningService,
    private val errorService: ErrorService,
    private val sessionCoroutineScope: CoroutineScope,
    private val dataStoreService: DataStoreService,
    private val storedCredentialValidator: StoredCredentialValidator,
) {
    private var periodicCheckJob: Job? = null
    private var storePruneJob: Job? = null

    private val browserReissue = BrowserReissueGuard()

    private val _refreshItems = MutableStateFlow<List<RefreshItem>>(emptyList())
    val refreshItems: StateFlow<List<RefreshItem>> = _refreshItems.asStateFlow()

    fun clearAllRefreshRequests() {
        _refreshItems.value = emptyList()
        browserReissue.clear()
    }

    fun removeRefreshRequest(item: RefreshItem) {
        _refreshItems.update { list -> list.filterNot { it.storeEntryId == item.storeEntryId } }
        browserReissue.release(item.storeEntryId)
    }

    /** Drops refresh requests whose credential no longer exists in the store. */
    private fun retainExisting(existingIds: Set<StoreEntryId>) {
        _refreshItems.update { list -> list.filter { it.storeEntryId in existingIds } }
    }

    suspend fun requestRefreshmentBatch(entriesWithIds: List<Pair<StoreEntryId, SubjectCredentialStore.StoreEntry>>) {
        val suppressedIds = getSuppressedRefreshCredentialIds()
        _refreshItems.update {
            entriesWithIds
                .filterNot { it.first in suppressedIds }
                .map { RefreshItem(storeEntryId = it.first, entry = it.second, scheme = it.second.resolveScheme()) }
        }
    }

    /**
     * Starts two background jobs: a periodic loop that queues expired credentials for refresh,
     * and an observer that drops queued requests once their credential is removed from the store.
     */
    fun startChecking(interval: Duration = 5.minutes) {
        periodicCheckJob?.cancel()
        periodicCheckJob = sessionCoroutineScope.launch {
            delay(10.seconds)
            while (isActive) {
                requestRefreshmentBatch(getInvalidCredentials())
                delay(interval)
            }
        }

        storePruneJob?.cancel()
        storePruneJob = sessionCoroutineScope.launch {
            subjectCredentialStore.observeStoreContainer().collect { storeContainer ->
                retainExisting(storeContainer.credentials.map { it.first }.toSet())
            }
        }

    }

    /**
     * The stored credentials that can be refreshed and need to, i.e. that are expired or verified to be revoked, see
     * [ui.models.CredentialFreshnessSummaryUiModel.needsRefresh], with their store entry IDs.
     */
    private suspend fun getInvalidCredentials(): List<Pair<StoreEntryId, SubjectCredentialStore.StoreEntry>> =
        coroutineScope {
            subjectCredentialStore.observeStoreContainer().first().credentials
                .filter { it.second.renewalInfo != null }
                .map { credential ->
                    credential to async {
                        catchingUnwrapped { storedCredentialValidator.freshness(credential.second).needsRefresh }
                            .getOrDefault(false)
                    }
                }
                .filter { (_, needsRefresh) -> needsRefresh.await() }
                .map { (credential, _) -> credential }
        }

    /**
     * Refreshes the credential
     */
    fun refreshSingle(entry: SubjectCredentialStore.StoreEntry, storeId: Long): Job = sessionCoroutineScope.launch {
        performRefreshLogic(entry, storeId)
    }

    /**
     * Wraps core refresh logic with status updates for [ui.views.RefreshCredentialsView]
     */
    fun refreshSingleWithStatus(item: RefreshItem): Job = sessionCoroutineScope.launch {
        if (item.status == RefreshStatus.InProgress) return@launch

        updateStatus(item.storeEntryId, RefreshStatus.InProgress)
        updateStatus(item.storeEntryId, performRefreshLogic(item.entry, item.storeEntryId))
    }

    private suspend fun performRefreshLogic(entry: SubjectCredentialStore.StoreEntry, storeId: Long): RefreshStatus =
        try {
            val renewalInfo = entry.renewalInfo ?: return RefreshStatus.Failed.also {
                    snackbarService.showSnackbar(
                        getString(Res.string.error_no_refresh_token),
                        entry.resolveScheme().uiLabelNonCompose()
                    )
                }

            provisioningService.refreshCredential(renewalInfo, storeId, ::updateStatus)
            snackbarService.showSnackbar(
                getString(Res.string.success_refreshed),
                entry.resolveScheme().uiLabelNonCompose()
            )
            RefreshStatus.Succeeded
        } catch (_: Exception) {
            handleReissueFallback(entry, storeId)
        }


    /**
     * Re-issues the credential with a new authorization in the browser. Only one such authorization can be open at a
     * time, as starting one makes [ProvisioningService] drop the one before as abandoned, whose redirect then fails.
     * So a credential requested while another one is still in the browser stays pending, to be refreshed afterwards.
     */
    private suspend fun handleReissueFallback(entry: SubjectCredentialStore.StoreEntry, storeId: Long): RefreshStatus {
        val renewalInfo = entry.renewalInfo ?: return RefreshStatus.Failed

        if (!browserReissue.tryAcquire(storeId)) {
            snackbarService.showSnackbar(
                getString(Res.string.error_reissue_in_progress),
                entry.resolveScheme().uiLabelNonCompose()
            )
            return RefreshStatus.Pending
        }
        return try {
            provisioningService.startProvisioningWithAuthRequest(
                credentialIssuer = renewalInfo.issuerMetadata.credentialIssuer,
                credentialIdentifierInfos = nonEmptyListOf(
                    CredentialIdentifierInfo(
                        issuerMetadata = renewalInfo.issuerMetadata,
                        credentialIdentifier = renewalInfo.credentialIdentifier,
                        supportedCredentialFormat = renewalInfo.credentialFormat
                    )
                ),
                reissuingStoreEntryId = storeId
            )
            RefreshStatus.InProgress
        } catch (e: Throwable) {
            browserReissue.release(storeId)
            errorService.emit(e)
            snackbarService.showSnackbar(
                getString(Res.string.error_reissue_failed),
                entry.resolveScheme().uiLabelNonCompose()
            )
            RefreshStatus.Failed
        }
    }

    fun updateStatus(id: Long, status: RefreshStatus, error: String? = null) {
        if (status == RefreshStatus.Succeeded || status == RefreshStatus.Failed) {
            browserReissue.release(id)
        }
        _refreshItems.update { list ->
            list.map { item ->
                if (item.storeEntryId == id) item.copy(status = status, error = error) else item
            }
        }
    }

    private suspend fun getSuppressedRefreshCredentialIds(): Set<StoreEntryId> =
        dataStoreService.getPreference(Configuration.DATASTORE_KEY_REFRESH_SUPPRESSED_CREDENTIALS)
            .first()
            ?.let {
                catchingUnwrapped { joseCompliantSerializer.decodeFromString<List<StoreEntryId>>(it).toSet() }
                    .getOrDefault(emptySet())
            }
            ?: emptySet()
}

data class RefreshItem(
    val storeEntryId: Long,
    val entry: SubjectCredentialStore.StoreEntry,
    val scheme: CredentialScheme,
    val selected: Boolean = true,
    val status: RefreshStatus = RefreshStatus.Pending,
    val error: String? = null
)

enum class RefreshStatus { Pending, InProgress, Succeeded, Failed }

/**
 * Lets one credential at a time be re-issued with an authorization in the browser. The credential holds the guard
 * until its redirect has been handled or its refresh request is removed. An authorization the user never returns
 * from would hold it for the rest of the session, so it is taken as abandoned after [abandonedAfter].
 */
internal class BrowserReissueGuard(
    private val abandonedAfter: Duration = 5.minutes,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    private val mutex = Mutex()
    @Volatile
    private var holder: Pair<StoreEntryId, TimeMark>? = null

    /** Whether [storeEntryId] may open the browser now, i.e. no other credential is waiting for its redirect. */
    suspend fun tryAcquire(storeEntryId: StoreEntryId): Boolean = mutex.withLock {
        val current = holder
        if (current != null && current.first != storeEntryId && current.second.elapsedNow() < abandonedAfter) {
            false
        } else {
            holder = storeEntryId to timeSource.markNow()
            true
        }
    }

    fun release(storeEntryId: StoreEntryId) {
        if (holder?.first == storeEntryId) {
            holder = null
        }
    }

    fun clear() {
        holder = null
    }
}
