package ui.screens

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.app.common.CredentialOfferInfo
import at.asitplus.wallet.app.common.ProvisioningService
import at.asitplus.wallet.app.common.WalletMain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class LoadCredentialViewModel {
    val walletMain: WalletMain
    val onSubmit: (ProvisioningService.CredentialIdentifierInfo, Set<NormalizedJsonPath>?) -> Unit
    val navigateUp: () -> Unit
    val hostString: String
    val credentialIdentifiers: Collection<ProvisioningService.CredentialIdentifierInfo>

    constructor(
        walletMain: WalletMain,
        onSubmit: (ProvisioningService.CredentialIdentifierInfo, Set<NormalizedJsonPath>?) -> Unit,
        navigateUp: () -> Unit,
        hostString: String
    ) {
        this.walletMain = walletMain
        this.onSubmit = onSubmit
        this.navigateUp = navigateUp
        this.hostString = hostString
        credentialIdentifiers = runBlocking {
            withContext(Dispatchers.IO) { walletMain.provisioningService.loadCredentialMetadata(hostString) }
        }
    }

    constructor(
        walletMain: WalletMain,
        offer: CredentialOfferInfo,
        onSubmit: (ProvisioningService.CredentialIdentifierInfo, Set<NormalizedJsonPath>?) -> Unit,
        navigateUp: () -> Unit,
    ) {
        this.walletMain = walletMain
        this.onSubmit = onSubmit
        this.navigateUp = navigateUp
        this.hostString = offer.credentialOffer.credentialIssuer
        // TODO select the credentialIdentifiers some other way?
        credentialIdentifiers = runBlocking {
            withContext(Dispatchers.IO) { walletMain.provisioningService.loadCredentialMetadata(hostString) }
        }
    }


}
