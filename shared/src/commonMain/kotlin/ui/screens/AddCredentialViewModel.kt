package ui.screens

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.data.ConstantIndex

class AddCredentialViewModel(val walletMain: WalletMain,
                             val onSubmit: (String, ConstantIndex.CredentialScheme, ConstantIndex.CredentialRepresentation, Set<NormalizedJsonPath>?) -> Unit,
                             val navigateUp: () -> Unit,
                             val hostString: String,
                             val availableSchemes: List<ConstantIndex.CredentialScheme>,
                             val showAttributes: Boolean = true) {

}