package at.asitplus.wallet.app.common

import at.asitplus.catchingUnwrapped
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.error_no_refresh_token
import at.asitplus.valera.resources.error_reissue_failed
import at.asitplus.valera.resources.success_refreshed
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.uiLabelNonCompose
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.ktor.openid.CredentialIdentifierInfo
import data.storage.DataStoreService
import data.storage.StoreEntryId
import data.storage.WalletSubjectCredentialStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class CredentialValidityService(
    private val subjectCredentialStore: WalletSubjectCredentialStore,
    private val snackbarService: SnackbarService,
    private val provisioningService: ProvisioningService,
    private val errorService: ErrorService,
    private val sessionCoroutineScope: CoroutineScope,
    private val dataStoreService: DataStoreService
) {
    private var periodicCheckJob: Job? = null
    private var storePruneJob: Job? = null

    private val _refreshItems = MutableStateFlow<List<RefreshItem>>(emptyList())
    val refreshItems: StateFlow<List<RefreshItem>> = _refreshItems.asStateFlow()

    fun clearAllRefreshRequests() {
        _refreshItems.value = emptyList()
    }

    fun removeRefreshRequest(item: RefreshItem) {
        _refreshItems.update { list -> list.filterNot { it.storeEntryId == item.storeEntryId } }
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
                .map { RefreshItem(storeEntryId = it.first, entry = it.second) }
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
                requestRefreshmentBatch(subjectCredentialStore.getInvalidCredentials())
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


    private suspend fun handleReissueFallback(entry: SubjectCredentialStore.StoreEntry, storeId: Long): RefreshStatus {
        val renewalInfo = entry.renewalInfo ?: return RefreshStatus.Failed

        return try {
            provisioningService.startProvisioningWithAuthRequest(
                credentialIssuer = renewalInfo.issuerMetadata.credentialIssuer,
                credentialIdentifierInfo = CredentialIdentifierInfo(
                    issuerMetadata = renewalInfo.issuerMetadata,
                    credentialIdentifier = renewalInfo.credentialIdentifier,
                    supportedCredentialFormat = renewalInfo.credentialFormat
                ),
                reissuingStoreEntryId = storeId
            )
            RefreshStatus.InProgress
        } catch (e: Throwable) {
            errorService.emit(e)
            snackbarService.showSnackbar(
                getString(Res.string.error_reissue_failed),
                entry.resolveScheme().uiLabelNonCompose()
            )
            RefreshStatus.Failed
        }
    }

    fun updateStatus(id: Long, status: RefreshStatus, error: String? = null) {
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
    val selected: Boolean = true,
    val status: RefreshStatus = RefreshStatus.Pending,
    val error: String? = null
)

enum class RefreshStatus { Pending, InProgress, Succeeded, Failed }
