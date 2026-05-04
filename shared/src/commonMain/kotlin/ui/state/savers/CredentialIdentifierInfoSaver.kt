package ui.state.savers

import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.wallet.lib.ktor.openid.CredentialIdentifierInfo

class CredentialIdentifierInfoSaver : ReusableSaver<CredentialIdentifierInfo, String>() {
    override fun prepareSaveable(value: CredentialIdentifierInfo) =
        joseCompliantSerializer.encodeToString(value)

    override fun restore(value: String) =
        joseCompliantSerializer.decodeFromString<CredentialIdentifierInfo>(value)
}



