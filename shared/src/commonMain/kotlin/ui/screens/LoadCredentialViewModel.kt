package ui.screens

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.openid.CredentialOfferGrantsPreAuthCodeTransactionCode
import at.asitplus.wallet.app.common.CredentialOfferInfo
import at.asitplus.wallet.app.common.ProvisioningService
import at.asitplus.wallet.app.common.WalletMain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * Selected transaction identifier, requested attributes, transaction code
 */
typealias CredentialSelection = (ProvisioningService.CredentialIdentifierInfo, Set<NormalizedJsonPath>?, String?) -> Unit

class LoadCredentialViewModel {
    val walletMain: WalletMain
    val onSubmit: CredentialSelection
    val navigateUp: () -> Unit
    val hostString: String
    val credentialIdentifiers: Collection<ProvisioningService.CredentialIdentifierInfo>
    val transactionCodeRequirements: CredentialOfferGrantsPreAuthCodeTransactionCode?

    constructor(
        walletMain: WalletMain,
        onSubmit: CredentialSelection,
        navigateUp: () -> Unit,
        hostString: String
    ) {
        this.walletMain = walletMain
        this.onSubmit = onSubmit
        this.navigateUp = navigateUp
        this.hostString = hostString
        this.transactionCodeRequirements = null
        credentialIdentifiers = runBlocking {
            withContext(Dispatchers.IO) { walletMain.provisioningService.loadCredentialMetadata(hostString) }
        }
    }

    constructor(
        walletMain: WalletMain,
        offer: CredentialOfferInfo,
        onSubmit: CredentialSelection,
        navigateUp: () -> Unit,
    ) {
        this.walletMain = walletMain
        this.onSubmit = onSubmit
        this.navigateUp = navigateUp
        this.hostString = offer.credentialOffer.credentialIssuer
        this.transactionCodeRequirements = offer.credentialOffer.grants?.preAuthorizedCode?.transactionCode
        credentialIdentifiers = runBlocking {
            withContext(Dispatchers.IO) {
                walletMain.provisioningService.loadCredentialMetadata(hostString)
                    //TODO.filter { it.credentialIdentifier in offer.credentialOffer.configurationIds }
            }
        }
    }


}
