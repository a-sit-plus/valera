package ui.viewmodels

import at.asitplus.openid.CredentialOffer
import at.asitplus.openid.CredentialOfferGrantsPreAuthCodeTransactionCode
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.ktor.openid.CredentialIdentifierInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * Selected transaction identifier, requested attributes, transaction code
 */
typealias CredentialSelection = (CredentialIdentifierInfo, String?) -> Unit

class LoadCredentialViewModel(
    val walletMain: WalletMain,
    val onSubmit: CredentialSelection,
    val navigateUp: () -> Unit,
    val hostString: String,
    val credentialIdentifiers: Collection<CredentialIdentifierInfo>,
    val transactionCodeRequirements: CredentialOfferGrantsPreAuthCodeTransactionCode?,
    val onClickLogo: () -> Unit,
    val onClickSettings: () -> Unit,
) {

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
            transactionCodeRequirements = null,
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
            transactionCodeRequirements = offer.grants?.preAuthorizedCode?.transactionCode,
            onClickLogo = onClickLogo,
            onClickSettings = onClickSettings,
            credentialIdentifiers = walletMain.scope.async {
                walletMain.provisioningService.loadCredentialMetadata(offer.credentialIssuer)
                    .filter { it.credentialIdentifier in offer.configurationIds }
            }.await()
        )
    }
}