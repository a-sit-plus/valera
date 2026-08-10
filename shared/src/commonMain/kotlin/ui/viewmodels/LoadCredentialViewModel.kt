package ui.viewmodels

import ErrorHandlingOverrideException
import at.asitplus.dcapi.issuance.DigitalCredentialOfferReturn
import at.asitplus.openid.CredentialOffer
import at.asitplus.openid.SupportedCredentialFormatIsoMdoc
import at.asitplus.openid.SupportedCredentialFormatSdJwt
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.wallet.app.common.LoadingMessageKey
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.dcapi.DCAPICredentialRepresentation
import at.asitplus.wallet.app.common.dcapi.DCAPICredentialType
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
    val requestedCredentialType: DCAPICredentialType?,
    val offer: CredentialOffer?,
    val onClickLogo: () -> Unit,
) {

    val initialCredentialIdentifierInfo: CredentialIdentifierInfo
        get() = credentialIdentifiers.first()

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
            requestedCredentialType: DCAPICredentialType? = null,
        ) = LoadCredentialViewModel(
            walletMain = walletMain,
            onSubmit = onSubmit,
            navigateUp = navigateUp,
            hostString = hostString,
            offer = null,
            requestedCredentialType = requestedCredentialType,
            onClickLogo = onClickLogo,
            credentialIdentifiers = walletMain.scope.async {
                onProgress?.invoke(LoadingMessageKey.IssuerMetadata)
                walletMain.provisioningService.loadCredentialMetadata(hostString)
                    .filterFor(requestedCredentialType)
                    .also {
                        if (requestedCredentialType != null && it.isEmpty()) {
                            throw RequestedCredentialTypeUnavailableException(hostString, requestedCredentialType)
                        }
                    }
            }.await()
        )

        suspend fun init(
            walletMain: WalletMain,
            offer: CredentialOffer,
            onSubmit: CredentialSelection,
            navigateUp: () -> Unit,
            onClickLogo: () -> Unit,
            onProgress: ((LoadingMessageKey) -> Unit)? = null,
        ) = LoadCredentialViewModel(
            walletMain = walletMain,
            onSubmit = onSubmit,
            navigateUp = navigateUp,
            hostString = offer.credentialIssuer,
            offer = offer,
            requestedCredentialType = null,
            onClickLogo = onClickLogo,
            credentialIdentifiers = walletMain.scope.async {
                onProgress?.invoke(LoadingMessageKey.IssuerMetadata)
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
            onProgress: ((LoadingMessageKey) -> Unit)? = null,
        ) = LoadCredentialViewModel(
            walletMain = walletMain,
            onSubmit = onSubmit,
            navigateUp = navigateUp,
            hostString = offer.credentialIssuer,
            offer = offer,
            requestedCredentialType = null,
            onClickLogo = onClickLogo,
            credentialIdentifiers = walletMain.scope.async {
                onProgress?.invoke(LoadingMessageKey.IssuerMetadata)
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
            onProgress: ((LoadingMessageKey) -> Unit)? = null,
        ): LoadCredentialViewModel {
            val offer = walletMain.scope.async {
                onProgress?.invoke(LoadingMessageKey.CredentialOffer)
                walletMain.provisioningService.decodeCredentialOffer(url)
            }.await()
            return LoadCredentialViewModel(
                walletMain = walletMain,
                onSubmit = onSubmit,
                navigateUp = navigateUp,
                hostString = offer.credentialIssuer,
                offer = offer,
                requestedCredentialType = null,
                onClickLogo = onClickLogo,
                credentialIdentifiers = walletMain.scope.async {
                    onProgress?.invoke(LoadingMessageKey.IssuerMetadata)
                    walletMain.provisioningService.loadCredentialMetadata(offer.credentialIssuer)
                        .filter { it.credentialIdentifier in offer.configurationIds }
                }.await()
            )
        }
    }
}

class RequestedCredentialTypeUnavailableException(
    host: String,
    val credentialType: DCAPICredentialType,
) : IllegalStateException("Issuer '$host' does not offer the requested credential type '${credentialType.type}'")

internal fun Collection<CredentialIdentifierInfo>.filterFor(
    requestedType: DCAPICredentialType?,
): List<CredentialIdentifierInfo> = if (requestedType == null) {
    toList()
} else {
    filter { identifier ->
        when (val format = identifier.supportedCredentialFormat) {
            is SupportedCredentialFormatIsoMdoc ->
                requestedType.representation == DCAPICredentialRepresentation.ISO_MDOC &&
                    format.docType == requestedType.type

            is SupportedCredentialFormatSdJwt ->
                requestedType.representation == DCAPICredentialRepresentation.SD_JWT &&
                    format.sdJwtVcType == requestedType.type

            else -> false
        }
    }
}
