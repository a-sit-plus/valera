package ui.viewmodels

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.asitplus.catchingUnwrapped
import at.asitplus.etsi.ListOfTrustedEntities
import at.asitplus.signum.indispensable.pki.X509Certificate
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.decodeImage
import at.asitplus.wallet.lib.etsi.LoTEFilterCriteria
import at.asitplus.wallet.lib.etsi.LoTEFilterService
import at.asitplus.wallet.lib.etsi.isTrustedBy
import data.storage.StoreEntryId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ui.models.CredentialFreshnessSummaryUiModel
import ui.models.toFallbackResolvedCredential
import ui.models.toResolvedCredential
import ui.models.toCredentialFreshnessSummaryModel
import ui.views.TrustState

class CredentialDetailsViewModel(
    val storeEntryId: StoreEntryId,
    val walletMain: WalletMain,
    val navigateUp: () -> Unit,
    val onClickLogo: () -> Unit
) : ViewModel() {

    private val loTeFilterService = LoTEFilterService()

    val imageDecoder: (ByteArray) -> Result<ImageBitmap> = { walletMain.platformAdapter.decodeImage(it) }

    val storeEntry = walletMain.subjectCredentialStore.observeStoreContainer().map { container ->
        container.credentials.find {
            it.first == storeEntryId
        }?.second?.let {
            catchingUnwrapped { it.toResolvedCredential() }
                .getOrElse { _ -> it.toFallbackResolvedCredential() }
        }
    }

    val credentialTimelinessesStates = channelFlow {
        val knownStates = mutableMapOf<Long, CredentialFreshnessSummaryUiModel>()
        walletMain.subjectCredentialStore.observeStoreContainer().collectLatest {
            coroutineScope {
                it.credentials.forEach {
                    launch {
                        knownStates[it.first] = walletMain.checkCredentialFreshness(it.second)
                            .toCredentialFreshnessSummaryModel()
                        send(knownStates.toMap())
                    }
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = mutableMapOf()
    )

    fun deleteStoreEntry() {
        walletMain.scope.launch(Dispatchers.IO) {
            walletMain.subjectCredentialStore.removeStoreEntryById(storeEntryId)
        }
    }

    val trustState: StateFlow<TrustState> = combine(
        storeEntry,
        walletMain.trustListStore.observeTrustContainer()
    ) { entry, trustContainerMap ->
        if (entry == null) return@combine TrustState.EVALUATING

        val issuerBytes = entry.issuer ?: return@combine TrustState.UNKNOWN

        val allLoTes = trustContainerMap.values.map { it.loTe }

        evaluateIssuer(issuerBytes, allLoTes, entry.schemaUri)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TrustState.EVALUATING
    )

    private fun evaluateIssuer(
        issuerBytes: ByteArray,
        trustLists: List<ListOfTrustedEntities>,
        serviceType: String
    ): TrustState {
        return try {
            val certificate = X509Certificate.decodeFromDer(issuerBytes)

            if (certificate.isTrustedBy(listOf(walletMain.trustListService.aistIssuerCert)).isSuccess) return TrustState.TRUSTED

            val criteria = LoTEFilterCriteria(expectedServiceType = serviceType)
            val certificateList: List<X509Certificate> = trustLists
                .flatMap { lote -> loTeFilterService.extractTrustedCertificates(lote, criteria) }
                .mapNotNull { it.certificate }

            if (certificateList.isEmpty()) {
                return TrustState.UNTRUSTED
            }

            val validationResult = certificate.isTrustedBy(certificateList)

            if (validationResult.isSuccess) TrustState.TRUSTED else TrustState.UNTRUSTED
        } catch (_: Exception) {
            TrustState.UNTRUSTED
        }
    }
}
