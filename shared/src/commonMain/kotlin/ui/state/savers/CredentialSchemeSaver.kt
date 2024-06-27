package ui.state.savers

import at.asitplus.wallet.app.common.third_party.at.asitplus.wallet.lib.data.identifier
import at.asitplus.wallet.app.common.third_party.at.asitplus.wallet.lib.data.resolveSchemeByIdentifier
import at.asitplus.wallet.lib.data.AttributeIndex
import at.asitplus.wallet.lib.data.ConstantIndex

class CredentialSchemeSaver : ReusableSaver<ConstantIndex.CredentialScheme, String>() {
    override fun prepareSaveable(value: ConstantIndex.CredentialScheme) = value.identifier

    override fun restore(value: String) = AttributeIndex.resolveSchemeByIdentifier(value)
        ?: throw Exception("Unknown credential scheme identifier: $value")
}



