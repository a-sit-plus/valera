package at.asitplus.wallet.app.common

import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.error_no_refresh_token
import at.asitplus.valera.resources.error_reissue_failed
import at.asitplus.valera.resources.success_refreshed
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.uiLabelNonCompose
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.ktor.openid.CredentialIdentifierInfo
import data.storage.StoreEntryId
import data.storage.WalletSubjectCredentialStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
) {
    private var job: Job? = null

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
    fun startChecking(interval: Duration = 5.minutes) {
        job?.cancel()

        job = sessionCoroutineScope.launch {
            delay(10.seconds)
            while (isActive) {
                requestRefreshmentBatch(subjectCredentialStore.getInvalidCredentials())
                delay(interval)
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
                        entry.scheme.uiLabelNonCompose()
                    )
                }

            provisioningService.refreshCredential(renewalInfo, storeId, ::updateStatus)
            snackbarService.showSnackbar(
                getString(Res.string.success_refreshed),
                entry.scheme.uiLabelNonCompose()
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
                entry.scheme.uiLabelNonCompose()
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
}

data class RefreshItem(
    val storeEntryId: Long,
    val entry: SubjectCredentialStore.StoreEntry,
    val selected: Boolean = true,
    val status: RefreshStatus = RefreshStatus.Pending,
    val error: String? = null
)

enum class RefreshStatus { Pending, InProgress, Succeeded, Failed }
