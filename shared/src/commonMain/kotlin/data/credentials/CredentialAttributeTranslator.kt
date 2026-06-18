package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.isEuPid
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.isMdl
import at.asitplus.wallet.lib.data.CredentialScheme
import org.jetbrains.compose.resources.StringResource

@Suppress("DEPRECATION")
interface CredentialAttributeTranslator {
    fun translateSingleClaimReference(claimReference: SingleClaimReference): StringResource?

    fun translate(attributeName: NormalizedJsonPath): StringResource?

    companion object {
        operator fun get(scheme: CredentialScheme?) = when {
            scheme.isEuPid -> EuPidCredentialAttributeTranslator()
            scheme.isMdl -> MobileDrivingLicenceCredentialAttributeTranslator()
            else -> null
        }
    }
}

