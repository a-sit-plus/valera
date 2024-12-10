package ui.state.savers

import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.ktor.openid.CredentialIdentifierInfo
import kotlinx.serialization.encodeToString

class CredentialIdentifierInfoSaver : ReusableSaver<CredentialIdentifierInfo, String>() {
    override fun prepareSaveable(value: CredentialIdentifierInfo) =
        vckJsonSerializer.encodeToString(value)

    override fun restore(value: String) =
        vckJsonSerializer.decodeFromString<CredentialIdentifierInfo>(value)
}



