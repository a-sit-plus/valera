package ui.state.savers

import at.asitplus.wallet.app.common.CredentialIdentifierInfo
import at.asitplus.wallet.app.common.ProvisioningService
import at.asitplus.wallet.lib.data.vckJsonSerializer
import kotlinx.serialization.encodeToString

class CredentialIdentifierInfoSaver : ReusableSaver<CredentialIdentifierInfo, String>() {
    override fun prepareSaveable(value: CredentialIdentifierInfo) =
        vckJsonSerializer.encodeToString(value)

    override fun restore(value: String) =
        vckJsonSerializer.decodeFromString<CredentialIdentifierInfo>(value)
}



