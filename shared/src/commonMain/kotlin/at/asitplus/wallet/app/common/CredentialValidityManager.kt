package at.asitplus.wallet.app.common

import at.asitplus.wallet.lib.agent.HolderAgent
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.multipaz.util.Platform.promptModel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class CredentialCheckManager(
    private val holderAgent: HolderAgent,
    private val provisioningService: ProvisioningService,
    private val snackbarService: SnackbarService,
) {
    private var job: Job? = null
    val coroutineExceptionHandler = CoroutineExceptionHandler { _, error -> }
    val scope =
        CoroutineScope(
            Dispatchers.Default + coroutineExceptionHandler + promptModel + CoroutineName(
                "WalletMain"
            )
        )
    /**
     * Starts a periodic loop that checks for expired credentials.
     */
    fun startChecking(interval: Duration = 10.seconds) {
        job?.cancel()

        job = scope.launch {
            while (isActive) {
                performCheck()
                delay(interval)
            }
        }
    }

    private suspend fun performCheck() {
        val credentials = holderAgent.getInvalidCredentials()
        credentials?.forEach {
            snackbarService.showSnackbar(it.getDcApiId())
        }
//        credentials?.forEach { storeEntry ->
//            storeEntry.refreshToken?.refreshToken?.let {
//                provisioningService.refreshCredential(storeEntry.refreshToken!!, storeEntry)
//            }
//        }
    }

    fun stopChecking() {
        job?.cancel()
    }
}