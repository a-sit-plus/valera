package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.isEuPid
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.isMdl
import at.asitplus.wallet.lib.data.CredentialScheme

@Suppress("DEPRECATION")
class CredentialAttributeTranslator private constructor(
    private val scheme: CredentialScheme,
) {
    fun translateSingleClaimReference(claimReference: SingleClaimReference): String? = when (claimReference) {
        is JsonClaimReference -> translate(claimReference.normalizedJsonPath)
        is MdocClaimReference -> translate(NormalizedJsonPath() + claimReference.namespace + claimReference.claimName)
    }

    fun translate(attributeName: NormalizedJsonPath): String? = scheme.metadataLabel(attributeName)

    companion object {
        operator fun get(scheme: CredentialScheme?): CredentialAttributeTranslator? = when {
            scheme == null -> null
            scheme.isEuPid || scheme.isMdl -> CredentialAttributeTranslator(scheme)
            else -> null
        }
    }
}
