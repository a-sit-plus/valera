package data.credentials

import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme

sealed interface CredentialAdapter {
    companion object {
        fun createFromStoreEntry(storeEntry: SubjectCredentialStore.StoreEntry): CredentialAdapter {
            return when (storeEntry.scheme) {
                is IdAustriaScheme -> IdAustriaCredentialAdapter.createFromStoreEntry(storeEntry)
                is EuPidScheme -> EuPidCredentialAdapter.createFromStoreEntry(storeEntry)
                is MobileDrivingLicenceScheme -> MobileDrivingLicenceCredentialAdapter.createFromStoreEntry(storeEntry)

                else -> throw IllegalArgumentException("storeEntry")
            }
        }

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