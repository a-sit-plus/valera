package ui.viewmodels

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.openid.CredentialOffer
import at.asitplus.openid.CredentialOfferGrantsPreAuthCodeTransactionCode
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.ktor.openid.CredentialIdentifierInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * Selected transaction identifier, requested attributes, transaction code
 */
typealias CredentialSelection = (CredentialIdentifierInfo, String?) -> Unit

class LoadCredentialViewModel {
    val walletMain: WalletMain
    val onSubmit: CredentialSelection
    val navigateUp: () -> Unit
    val hostString: String
    val credentialIdentifiers: Collection<CredentialIdentifierInfo>
    val transactionCodeRequirements: CredentialOfferGrantsPreAuthCodeTransactionCode?
    val onClickLogo: () -> Unit

    constructor(
        walletMain: WalletMain,
        onSubmit: CredentialSelection,
        navigateUp: () -> Unit,
        hostString: String,
        onClickLogo: () -> Unit
    ) {
        this.walletMain = walletMain
        this.onSubmit = onSubmit
        this.navigateUp = navigateUp
        this.hostString = hostString
        this.transactionCodeRequirements = null
        this.onClickLogo = onClickLogo
        credentialIdentifiers = runBlocking {
            withContext(Dispatchers.IO) {
                walletMain.provisioningService.loadCredentialMetadata(hostString)
            }
        }
    }

    constructor(
        walletMain: WalletMain,
        offer: CredentialOffer,
        onSubmit: CredentialSelection,
        navigateUp: () -> Unit,
        onClickLogo: () -> Unit
    ) {
        this.walletMain = walletMain
        this.onSubmit = onSubmit
        this.navigateUp = navigateUp
        this.hostString = offer.credentialIssuer
        this.transactionCodeRequirements = offer.grants?.preAuthorizedCode?.transactionCode
        this.onClickLogo = onClickLogo
        credentialIdentifiers = runBlocking {
            withContext(Dispatchers.IO) {
                walletMain.provisioningService.loadCredentialMetadata(hostString)
                    .filter { it.credentialIdentifier in offer.configurationIds }
            }
        }
    }
}