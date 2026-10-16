package ui.viewmodels

import ErrorHandlingOverrideException
import at.asitplus.dcapi.issuance.DigitalCredentialOfferReturn
import at.asitplus.openid.CredentialOffer
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.wallet.app.common.LoadingMessageKey
import at.asitplus.wallet.app.common.ProvisioningService
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.validation.IssuerValidationUi
import at.asitplus.wallet.lib.oidvci.CredentialIdentifierInfo
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
    /** The credential issuer as validated from its metadata, `null` if it has not been validated. */
    val issuer: IssuerValidationUi? = null,
) {

    init {
        check(credentialIdentifiers.isNotEmpty()) {
            "Issuer '$hostString' did not provide any credential configuration that can be loaded"
        }
    }

    fun handleDCAPIIssuingResult(success: Boolean, error: Throwable? = null) {
        if (!walletMain.platformAdapter.hasPendingDCAPIIssuingRequest()) {
            return
        }
        if (!success) {
            val deferredError = ErrorHandlingOverrideException(
                onAcknowledge = {
                    if (!walletMain.platformAdapter.hasPendingDCAPIIssuingRequest()) {
                        return@ErrorHandlingOverrideException
                    }
                    // TODO replace with official status messages once specification defines them
                    val response =
                        joseCompliantSerializer.encodeToString(DigitalCredentialOfferReturn.error(status = "offer_declined"))
                    walletMain.platformAdapter.prepareDCAPIIssuingResponse(response, false)
                    navigateUp()
                },
                cause = error ?: Exception("Issuance failed")
            )
            walletMain.errorService.emit(deferredError)
        } else {
            val response = joseCompliantSerializer.encodeToString(DigitalCredentialOfferReturn.success())
            walletMain.platformAdapter.prepareDCAPIIssuingResponse(response, true)
            navigateUp()
        }
    }

    companion object {
        suspend fun init(
            walletMain: WalletMain,
            onSubmit: CredentialSelection,
            navigateUp: () -> Unit,
            hostString: String,
            onClickLogo: () -> Unit,
            onProgress: ((LoadingMessageKey) -> Unit)? = null,
        ): LoadCredentialViewModel {
            val metadata = walletMain.scope.async {
                onProgress?.invoke(LoadingMessageKey.IssuerMetadata)
                walletMain.provisioningService.loadCredentialMetadata(hostString)
            }.await()
            return LoadCredentialViewModel(
                walletMain = walletMain,
                onSubmit = onSubmit,
                navigateUp = navigateUp,
                hostString = hostString,
                offer = null,
                onClickLogo = onClickLogo,
                credentialIdentifiers = metadata.credentialIdentifiers,
                issuer = metadata.issuer,
            )
        }

        suspend fun init(
            walletMain: WalletMain,
            offer: CredentialOffer,
            onSubmit: CredentialSelection,
            navigateUp: () -> Unit,
            onClickLogo: () -> Unit,
            onProgress: ((LoadingMessageKey) -> Unit)? = null,
        ): LoadCredentialViewModel {
            val metadata = walletMain.scope.async {
                onProgress?.invoke(LoadingMessageKey.IssuerMetadata)
                walletMain.provisioningService.loadCredentialMetadata(offer.credentialIssuer)
            }.await()
            return forOffer(walletMain, offer, metadata, onSubmit, navigateUp, onClickLogo)
        }

        suspend fun initFromDcApi(
            walletMain: WalletMain,
            offer: CredentialOffer,
            onSubmit: CredentialSelection,
            navigateUp: () -> Unit,
            onClickLogo: () -> Unit,
            onProgress: ((LoadingMessageKey) -> Unit)? = null,
        ): LoadCredentialViewModel {
            val metadata = walletMain.scope.async {
                onProgress?.invoke(LoadingMessageKey.IssuerMetadata)
                val issuerMetadata = requireNotNull(offer.credentialIssuerMetadata) {
                    "Missing credential issuer metadata for DC API request"
                }
                walletMain.provisioningService.parseCredentialMetadata(issuerMetadata)
            }.await()
            return forOffer(walletMain, offer, metadata, onSubmit, navigateUp, onClickLogo)
        }

        suspend fun init(
            walletMain: WalletMain,
            url: String,
            onSubmit: CredentialSelection,
            navigateUp: () -> Unit,
            onClickLogo: () -> Unit,
            onProgress: ((LoadingMessageKey) -> Unit)? = null,
        ): LoadCredentialViewModel {
            val offer = walletMain.scope.async {
                onProgress?.invoke(LoadingMessageKey.CredentialOffer)
                walletMain.provisioningService.decodeCredentialOffer(url)
            }.await()
            return init(walletMain, offer, onSubmit, navigateUp, onClickLogo, onProgress)
        }

        /** Offers only the credentials of [offer], see [CredentialOffer.configurationIds]. */
        private fun forOffer(
            walletMain: WalletMain,
            offer: CredentialOffer,
            metadata: ProvisioningService.IssuerMetadataLoad,
            onSubmit: CredentialSelection,
            navigateUp: () -> Unit,
            onClickLogo: () -> Unit,
        ) = LoadCredentialViewModel(
            walletMain = walletMain,
            onSubmit = onSubmit,
            navigateUp = navigateUp,
            hostString = offer.credentialIssuer,
            offer = offer,
            onClickLogo = onClickLogo,
            credentialIdentifiers = metadata.credentialIdentifiers
                .filter { it.credentialIdentifier in offer.configurationIds },
            issuer = metadata.issuer,
        )
    }
}
