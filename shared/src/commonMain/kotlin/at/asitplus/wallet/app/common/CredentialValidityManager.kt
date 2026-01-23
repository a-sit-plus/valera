package at.asitplus.wallet.app.common

import at.asitplus.wallet.lib.agent.HolderAgent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class CredentialCheckManager(
    private val holderAgent: HolderAgent,
    private val provisioningService: ProvisioningService,
    private val scope: CoroutineScope,
    private val walletMain: WalletMain
) {
    private var job: Job? = null

    /**
     * Starts a periodic loop that checks for expired credentials.
     */
    fun startChecking(interval: Duration = 30.minutes) {
        job?.cancel()

        job = scope.launch {
            while (isActive) {
                performCheck()
                delay(interval)
            }
        }
    }

    private suspend fun performCheck() {
//        holderAgent.getInvalidCredentials()?.forEach { (entry, refreshEntry) ->
//            val info = refreshEntry.credentialIdentifierInfo
//
//            if (refreshEntry.refreshTokenInfo != null) {
//                runCatching {
//                    provisioningService.refreshCredential(refreshEntry.refreshTokenInfo!!, entry)
//                }.onFailure {
//                    walletMain.triggerManualRenewal(info)
//                }
//            } else {
//                walletMain.triggerManualRenewal(info)
//            }
//        }
    }

    fun stopChecking() {
        job?.cancel()
    }
}