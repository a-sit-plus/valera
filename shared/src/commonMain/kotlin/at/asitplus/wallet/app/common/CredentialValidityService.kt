package at.asitplus.wallet.app.common

import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.uiLabelNonCompose
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.ktor.openid.CredentialIdentifierInfo
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.multipaz.util.Platform.promptModel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class CredentialValidityService(
    private val holderAgent: HolderAgent,
    private val snackbarService: SnackbarService,
    private val provisioningService: ProvisioningService,
    private val errorService: ErrorService
) {
    private var job: Job? = null
    val coroutineExceptionHandler = CoroutineExceptionHandler { _, error -> }
    val scope =
        CoroutineScope(
            Dispatchers.Default + coroutineExceptionHandler + promptModel + CoroutineName(
                "WalletMain"
            )
        )
    private val _refreshItems = MutableStateFlow<List<RefreshItem>>(emptyList())
    val refreshItems: StateFlow<List<RefreshItem>> = _refreshItems.asStateFlow()

    private fun entryId(item: RefreshItem): String = item.storeId.toString()

    fun clearAllRefreshRequests() {
        _refreshItems.value = emptyList()
    }

    fun removeRefreshRequest(item: RefreshItem) {
        val id = entryId(item)
        _refreshItems.update { list -> list.filterNot { entryId(it) == id } }
    }

    fun requestRefreshmentBatch(entriesWithIds: List<Pair<Long, SubjectCredentialStore.StoreEntry>>) {
        _refreshItems.update { entriesWithIds.map { RefreshItem(storeId = it.first, entry = it.second) } }
    }

    /**
     * Starts a periodic loop that checks for expired credentials.
     */
    fun startChecking(interval: Duration = 40.seconds) {
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
        val invalid = holderAgent.getInvalidCredentials()
        requestRefreshmentBatch(invalid)
    }

    suspend fun startProvisioningAwait(
        host: String,
        credentialIdentifierInfo: CredentialIdentifierInfo
    ): Boolean {
        return try {
            provisioningService.startProvisioningWithAuthRequest(
                credentialIssuer = host,
                credentialIdentifierInfo = credentialIdentifierInfo,
            )
            true
        } catch (e: Throwable) {
            errorService.emit(e)
            false
        }
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
        val stableId = entryId(item)
        if (item.status == RefreshStatus.InProgress) return@launch

        updateStatus(stableId, RefreshStatus.InProgress)

        val success = performRefreshLogic(item.entry, item.storeId)

        if (success) {
            updateStatus(stableId, RefreshStatus.Succeeded)
        } else {
            updateStatus(stableId, RefreshStatus.Failed, "Refresh failed")
        }
    }

    private suspend fun performRefreshLogic(entry: SubjectCredentialStore.StoreEntry, storeId: Long): Boolean {
        return try {
            val refreshToken = entry.refreshToken
            if (refreshToken == null) {
                snackbarService.showSnackbar("No refresh token available for ${entry.scheme.uiLabelNonCompose()}")
                return false
            }

            provisioningService.refreshCredential(refreshToken, storeId)
            snackbarService.showSnackbar("Refreshed ${entry.scheme.uiLabelNonCompose()}")
            true
        } catch (_: Exception) {
            handleReissueFallback(entry, storeId)
        }
    }

    private suspend fun handleReissueFallback(entry: SubjectCredentialStore.StoreEntry, storeId: Long): Boolean {
        val rt = entry.refreshToken ?: return false

        val ok = startProvisioningAwait(
            host = rt.issuerMetadata.credentialIssuer,
            credentialIdentifierInfo = CredentialIdentifierInfo(
                issuerMetadata = rt.issuerMetadata,
                credentialIdentifier = rt.credentialIdentifier,
                supportedCredentialFormat = rt.credentialFormat
            )
        )

        return if (ok) {
            holderAgent.deleteCredential(storeId)
            snackbarService.showSnackbar("${entry.scheme.uiLabelNonCompose()} re-issued")
            true
        } else {
            snackbarService.showSnackbar("Re-issue failed for ${entry.scheme.uiLabelNonCompose()}")
            false
        }
    }

    private fun updateStatus(id: String, status: RefreshStatus, error: String? = null) {
        _refreshItems.update { list ->
            list.map { item ->
                if (entryId(item) == id) item.copy(status = status, error = error) else item
            }
        }
    }
}

data class RefreshItem(
    val storeId: Long,
    val entry: SubjectCredentialStore.StoreEntry,
    val selected: Boolean = true,
    val status: RefreshStatus = RefreshStatus.Pending,
    val error: String? = null
)

enum class RefreshStatus { Pending, InProgress, Succeeded, Failed }
