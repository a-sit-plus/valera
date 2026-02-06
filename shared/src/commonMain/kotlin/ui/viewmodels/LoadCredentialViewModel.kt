package ui.viewmodels

import DeferredErrorActionException
import at.asitplus.dcapi.issuance.DigitalCredentialOfferReturn
import at.asitplus.openid.CredentialOffer
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.ktor.openid.CredentialIdentifierInfo
import kotlinx.coroutines.async

/**
 * Selected transaction identifier, requested attributes, transaction code
 */
typealias CredentialSelection = (CredentialIdentifierInfo, String?, CredentialOffer?) -> Unit

class LoadCredentialViewModel(
    val walletMain: WalletMain,
    val onSubmit: CredentialSelection,
    val navigateUp: () -> Unit,
    val hostString: String,
    val credentialIdentifiers: Collection<CredentialIdentifierInfo>,
    val offer: CredentialOffer?,
    val onClickLogo: () -> Unit,
    val onClickSettings: () -> Unit,
) {
    fun handleDCAPICreationResult(success: Boolean, error: Throwable? = null) {
        if (!walletMain.platformAdapter.hasPendingDCAPICreationRequest()) {
            return
        }
        if (!success) {
            val deferredError = DeferredErrorActionException(
                onAcknowledge = {
                    if (!walletMain.platformAdapter.hasPendingDCAPICreationRequest()) {
                        return@DeferredErrorActionException
                    }
                    // TODO replace with official status messages once specification defines them
                    val response =
                        vckJsonSerializer.encodeToString(DigitalCredentialOfferReturn.error(status = "offer_declined"))
                    walletMain.platformAdapter.prepareDCAPICreationResponse(response, false)
                    walletMain.platformAdapter.finishApp()
                },
                cause = error ?: Exception("issuance failed")
            )
            walletMain.errorService.emit(deferredError)
        } else {
            val response = vckJsonSerializer.encodeToString(DigitalCredentialOfferReturn.success())
            walletMain.platformAdapter.prepareDCAPICreationResponse(response, true)
            walletMain.platformAdapter.finishApp()
        }
    }

    companion object {
        suspend fun init(
            walletMain: WalletMain,
            onSubmit: CredentialSelection,
            navigateUp: () -> Unit,
            hostString: String,
            onClickLogo: () -> Unit,
            onClickSettings: () -> Unit
        ) = LoadCredentialViewModel(
            walletMain = walletMain,
            onSubmit = onSubmit,
            navigateUp = navigateUp,
            hostString = hostString,
            offer = null,
            onClickLogo = onClickLogo,
            onClickSettings = onClickSettings,
            credentialIdentifiers = walletMain.scope.async {
                walletMain.provisioningService.loadCredentialMetadata(hostString)
            }.await()
        )

        suspend fun init(
            walletMain: WalletMain,
            offer: CredentialOffer,
            onSubmit: CredentialSelection,
            navigateUp: () -> Unit,
            onClickLogo: () -> Unit,
            onClickSettings: () -> Unit
        ) = LoadCredentialViewModel(
            walletMain = walletMain,
            onSubmit = onSubmit,
            navigateUp = navigateUp,
            hostString = offer.credentialIssuer,
            offer = offer,
            onClickLogo = onClickLogo,
            onClickSettings = onClickSettings,
            credentialIdentifiers = walletMain.scope.async {
                walletMain.provisioningService.loadCredentialMetadata(offer.credentialIssuer)
                    .filter { it.credentialIdentifier in offer.configurationIds }
            }.await()
        )

        suspend fun initFromDcApi(
            walletMain: WalletMain,
            offer: CredentialOffer,
            onSubmit: CredentialSelection,
            navigateUp: () -> Unit,
            onClickLogo: () -> Unit,
            onClickSettings: () -> Unit
        ) = LoadCredentialViewModel(
            walletMain = walletMain,
            onSubmit = onSubmit,
            navigateUp = navigateUp,
            hostString = offer.credentialIssuer,
            offer = offer,
            onClickLogo = onClickLogo,
            onClickSettings = onClickSettings,
            credentialIdentifiers = walletMain.scope.async {
                val issuerMetadata = requireNotNull(offer.credentialIssuerMetadata) {
                    "Missing credential issuer metadata for DC API request"
                }
                walletMain.provisioningService.parseCredentialMetadata(issuerMetadata)
                    .filter { it.credentialIdentifier in offer.configurationIds }
            }.await()
        )
        suspend fun init(
            walletMain: WalletMain,
            url: String,
            onSubmit: CredentialSelection,
            navigateUp: () -> Unit,
            onClickLogo: () -> Unit,
            onClickSettings: () -> Unit
        ): LoadCredentialViewModel {
            val offer = walletMain.scope.async {
                walletMain.provisioningService.decodeCredentialOffer(url)
            }.await()
            return LoadCredentialViewModel(
                walletMain = walletMain,
                onSubmit = onSubmit,
                navigateUp = navigateUp,
                hostString = offer.credentialIssuer,
                offer = offer,
                onClickLogo = onClickLogo,
                onClickSettings = onClickSettings,
                credentialIdentifiers = walletMain.scope.async {
                    walletMain.provisioningService.loadCredentialMetadata(offer.credentialIssuer)
                        .filter { it.credentialIdentifier in offer.configurationIds }
                }.await()
            )
        }
    }
}
