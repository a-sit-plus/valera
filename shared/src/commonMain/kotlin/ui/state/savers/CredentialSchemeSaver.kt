package ui.state.savers

import at.asitplus.wallet.lib.data.AttributeIndex
import at.asitplus.wallet.lib.data.ConstantIndex

class CredentialSchemeSaver : ReusableSaver<ConstantIndex.CredentialScheme, String>() {
    override fun prepareSaveable(value: ConstantIndex.CredentialScheme): String {
        return value.vcType?:"TODO2 UNKNOWN"
    }

    override fun restore(value: String): ConstantIndex.CredentialScheme {
        val scheme =
            AttributeIndex.resolveAttributeType(value) ?: throw Exception("Unknown vc type: $value")
        return scheme
    }
}



