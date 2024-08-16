package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex
import data.Attribute

sealed interface CredentialAdapter {
    val scheme: ConstantIndex.CredentialScheme
    fun getAttribute(path: NormalizedJsonPath): Attribute?

    companion object {
        fun SubjectCredentialStore.StoreEntry.SdJwt.toAttributeMap() =
            disclosures.values.filterNotNull().associate {
                it.claimName to it.claimValue
            }

        fun SubjectCredentialStore.StoreEntry.Iso.toNamespaceAttributeMap() =
            issuerSigned.namespaces?.mapValues { namespace ->
                namespace.value.entries.associate {
                    it.value.elementIdentifier to it.value.elementValue
                }
            }
    }
}