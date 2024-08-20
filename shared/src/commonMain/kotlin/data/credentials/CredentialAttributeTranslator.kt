package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import org.jetbrains.compose.resources.StringResource

interface CredentialAttributeTranslator {
    fun translate(attributeName: NormalizedJsonPath): StringResource?

    companion object {
        fun getSchemeTranslator(scheme: ConstantIndex.CredentialScheme) = when(scheme) {
            is IdAustriaScheme -> IdAustriaCredentialAttributeTranslator
            is EuPidScheme -> EuPidCredentialAttributeTranslator
            is MobileDrivingLicenceScheme -> MobileDrivingLicenceCredentialAttributeTranslator
            else -> null
        }
    }
}

