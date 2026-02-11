package at.asitplus.wallet.app.common

import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.error_no_refresh_token
import at.asitplus.valera.resources.error_reissue_failed
import at.asitplus.valera.resources.success_refreshed
import at.asitplus.valera.resources.success_reissued
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.uiLabelNonCompose
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.ktor.openid.CredentialIdentifierInfo
import data.storage.StoreEntryId
import data.storage.WalletSubjectCredentialStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class CredentialValidityService(
    private val subjectCredentialStore: WalletSubjectCredentialStore,
    private val snackbarService: SnackbarService,
    private val provisioningService: ProvisioningService,
    private val errorService: ErrorService
) {
    private var job: Job? = null
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _refreshItems = MutableStateFlow<List<RefreshItem>>(emptyList())
    val refreshItems: StateFlow<List<RefreshItem>> = _refreshItems.asStateFlow()

    fun clearAllRefreshRequests() { _refreshItems.value = emptyList() }

    fun removeRefreshRequest(item: RefreshItem) {
        _refreshItems.update { list -> list.filterNot { it.storeEntryId == item.storeEntryId } }
    }

    fun requestRefreshmentBatch(entriesWithIds: List<Pair<StoreEntryId, SubjectCredentialStore.StoreEntry>>) {
        _refreshItems.update { entriesWithIds.map { RefreshItem(storeEntryId = it.first, entry = it.second) } }
    }

    /**
     * Starts a periodic loop that checks for expired credentials.
     */
    fun startChecking(interval: Duration = 30.seconds) {
        job?.cancel()

        job = scope.launch {
            delay(10.seconds)
            while (isActive) {
                performCheck()
                delay(interval)
            }
        }
    }

    private suspend fun performCheck() {
        val invalid = subjectCredentialStore.getInvalidCredentials()
        requestRefreshmentBatch(invalid)
    }

    suspend fun startProvisioningForReissuance(host: String, credentialIdentifierInfo: CredentialIdentifierInfo, reissuingStoreEntryId: StoreEntryId): Boolean = try {
        provisioningService.startProvisioningWithAuthRequest(host, credentialIdentifierInfo, reissuingStoreEntryId)
        true
    } catch (e: Throwable) {
        errorService.emit(e)
        false
    }

    /**
     * Refreshes the credential
     */
    fun refreshSingle(entry: SubjectCredentialStore.StoreEntry, storeId: Long): Job = scope.launch {
        performRefreshLogic(entry, storeId)
    }

    /**
     * Wraps core refresh logic with status updates for [ui.views.RefreshCredentialsView]
     */
    fun refreshSingleWithStatus(item: RefreshItem): Job = scope.launch {
        if (item.status == RefreshStatus.InProgress) return@launch

        updateStatus(item.storeEntryId, RefreshStatus.InProgress)

        val status = performRefreshLogic(item.entry, item.storeEntryId)
        updateStatus(item.storeEntryId, status)
    }

    private suspend fun performRefreshLogic(entry: SubjectCredentialStore.StoreEntry, storeId: Long): RefreshStatus {
        return try {
            val refreshToken = entry.refreshToken
            if (refreshToken == null) {
                snackbarService.showSnackbar(getString(Res.string.error_no_refresh_token), entry.scheme.uiLabelNonCompose())
                return RefreshStatus.Failed
            }

            provisioningService.refreshCredential(refreshToken, storeId)
            snackbarService.showSnackbar(getString(Res.string.success_refreshed), entry.scheme.uiLabelNonCompose())
            RefreshStatus.Succeeded
        } catch (_: Exception) {
            handleReissueFallback(entry, storeId)
        }
    }

    private suspend fun handleReissueFallback(entry: SubjectCredentialStore.StoreEntry, storeId: Long): RefreshStatus {
        val rt = entry.refreshToken ?: return RefreshStatus.Failed

        val ok = startProvisioningForReissuance(
            host = rt.issuerMetadata.credentialIssuer,
            credentialIdentifierInfo = CredentialIdentifierInfo(
                issuerMetadata = rt.issuerMetadata,
                credentialIdentifier = rt.credentialIdentifier,
                supportedCredentialFormat = rt.credentialFormat
            ),
            reissuingStoreEntryId = storeId
        )

        return if (!ok) {
            snackbarService.showSnackbar(getString(Res.string.error_reissue_failed), entry.scheme.uiLabelNonCompose())
            RefreshStatus.Failed
        } else {
            RefreshStatus.InProgress
        }
    }

    fun updateStatus(id: Long, status: RefreshStatus, error: String? = null) {
        _refreshItems.update { list ->
            list.map { item ->
                if (item.storeEntryId == id) item.copy(status = status, error = error) else item
            }
        }
    }
}

data class RefreshItem(
    val storeEntryId: Long,
    val entry: SubjectCredentialStore.StoreEntry,
    val selected: Boolean = true,
    val status: RefreshStatus = RefreshStatus.Pending,
    val error: String? = null
)

enum class RefreshStatus { Pending, InProgress, Succeeded, Failed }
