package ui.state.savers

import at.asitplus.wallet.app.common.ProvisioningService
import at.asitplus.wallet.lib.data.vckJsonSerializer
import kotlinx.serialization.encodeToString

class CredentialIdentifierInfoSaver : ReusableSaver<ProvisioningService.CredentialIdentifierInfo, String>() {
    override fun prepareSaveable(value: ProvisioningService.CredentialIdentifierInfo) =
        vckJsonSerializer.encodeToString(value)

    override fun restore(value: String) =
        vckJsonSerializer.decodeFromString<ProvisioningService.CredentialIdentifierInfo>(value)
}



