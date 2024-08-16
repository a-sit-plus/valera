package data.credentials

import at.asitplus.wallet.idaustria.IdAustriaCredential
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.credentials.CredentialAdapter.Companion.toAttributeMap
import data.credentials.CredentialAdapter.Companion.toNamespaceAttributeMap
import io.ktor.util.decodeBase64Bytes
import kotlinx.datetime.LocalDate

sealed interface IdAustriaCredentialAdapter : CredentialAdapter {
    val bpk: String
    val givenName: String
    val familyName: String
    val dateOfBirth: LocalDate
    val portrait: ByteArray?
    val ageAtLeast14: Boolean?
    val ageAtLeast16: Boolean?
    val ageAtLeast18: Boolean?
    val ageAtLeast21: Boolean?
    val mainAddressRaw: String?

    companion object {
        fun createFromStoreEntry(storeEntry: SubjectCredentialStore.StoreEntry): IdAustriaCredentialAdapter {
            if (storeEntry.scheme !is IdAustriaScheme) {
                throw IllegalArgumentException("credential")
            }
            return when (storeEntry) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    (storeEntry.vc.vc.credentialSubject as? IdAustriaCredential)?.let {
                        IdAustriaCredentialVcAdapter(it)
                    } ?: throw IllegalArgumentException("credential")
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    IdAustriaCredentialSdJwtAdapter(
                        storeEntry.toAttributeMap(),
                    )
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    IdAustriaCredentialIsoMdocAdapter(
                        storeEntry.toNamespaceAttributeMap(),
                    )
                }
            }
        }
    }
}

private class IdAustriaCredentialVcAdapter(
    val credentialSubject: IdAustriaCredential
) : IdAustriaCredentialAdapter {
    override val bpk: String
        get() = credentialSubject.bpk

    override val givenName: String
        get() = credentialSubject.firstname

    override val familyName: String
        get() = credentialSubject.lastname

    override val dateOfBirth: LocalDate
        get() = credentialSubject.dateOfBirth

    override val portrait: ByteArray?
        get() = credentialSubject.portrait

    override val ageAtLeast14: Boolean?
        get() = credentialSubject.ageOver14

    override val ageAtLeast16: Boolean?
        get() = credentialSubject.ageOver16

    override val ageAtLeast18: Boolean?
        get() = credentialSubject.ageOver18

    override val ageAtLeast21: Boolean?
        get() = credentialSubject.ageOver21

    override val mainAddressRaw: String?
        get() = credentialSubject.mainAddress
}

private class IdAustriaCredentialSdJwtAdapter(
    val attributes: Map<String, Any>
) : IdAustriaCredentialAdapter {
    override val bpk: String
        get() = attributes[IdAustriaScheme.Attributes.BPK] as String

    override val givenName: String
        get() = attributes[IdAustriaScheme.Attributes.FIRSTNAME] as String

    override val familyName: String
        get() = attributes[IdAustriaScheme.Attributes.LASTNAME] as String

    override val dateOfBirth: LocalDate
        get() = attributes[IdAustriaScheme.Attributes.DATE_OF_BIRTH].let {
            LocalDate.parse(it as String)
        }

    override val portrait: ByteArray?
        get() = attributes[IdAustriaScheme.Attributes.PORTRAIT]?.let {
            (it as String).decodeBase64Bytes()
        }

    override val ageAtLeast14: Boolean?
        get() = attributes[IdAustriaScheme.Attributes.AGE_OVER_14] as Boolean?

    override val ageAtLeast16: Boolean?
        get() = attributes[IdAustriaScheme.Attributes.AGE_OVER_16] as Boolean?

    override val ageAtLeast18: Boolean?
        get() = attributes[IdAustriaScheme.Attributes.AGE_OVER_18] as Boolean?

    override val ageAtLeast21: Boolean?
        get() = attributes[IdAustriaScheme.Attributes.AGE_OVER_21] as Boolean?

    override val mainAddressRaw: String?
        get() = attributes[IdAustriaScheme.Attributes.MAIN_ADDRESS] as String?
}

private class IdAustriaCredentialIsoMdocAdapter(
    namespaces: Map<String, Map<String, Any>>?,
) : IdAustriaCredentialAdapter {
    private val idAustriaNamespace = namespaces?.get(IdAustriaScheme.isoNamespace)
        ?: throw IllegalArgumentException("namespaces") // contains required attributes

    private val idAustriaNamespaceProxy = IdAustriaCredentialSdJwtAdapter(idAustriaNamespace)

    override val bpk: String
        get() = idAustriaNamespaceProxy.bpk

    override val givenName: String
        get() = idAustriaNamespaceProxy.givenName

    override val familyName: String
        get() = idAustriaNamespaceProxy.familyName

    override val dateOfBirth: LocalDate
        get() = idAustriaNamespaceProxy.dateOfBirth

    override val portrait: ByteArray?
        get() = idAustriaNamespaceProxy.portrait

    override val ageAtLeast14: Boolean?
        get() = idAustriaNamespaceProxy.ageAtLeast14

    override val ageAtLeast16: Boolean?
        get() = idAustriaNamespaceProxy.ageAtLeast16

    override val ageAtLeast18: Boolean?
        get() = idAustriaNamespaceProxy.ageAtLeast18

    override val ageAtLeast21: Boolean?
        get() = idAustriaNamespaceProxy.ageAtLeast21

    override val mainAddressRaw: String?
        get() = idAustriaNamespaceProxy.mainAddressRaw
}