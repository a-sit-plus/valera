package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.eprescription.EPrescriptionDataElements.OTT
import at.asitplus.wallet.eprescription.EPrescriptionDataElements.VALID_UNTIL
import at.asitplus.wallet.eprescription.EPrescriptionDataElements.COUNTRY_CODE
import at.asitplus.wallet.eprescription.EPrescriptionScheme
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.Attribute
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

sealed class EPrescriptionCredentialAdapter : CredentialAdapter() {
    override fun getAttribute(path: NormalizedJsonPath) = path.segments.firstOrNull()?.let { first ->
        when (first) {
            is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
                VALID_UNTIL -> Attribute.fromValue(validUntil)
                OTT -> Attribute.fromValue(ott)
                COUNTRY_CODE -> Attribute.fromValue(countryCode)
                else -> null
            }
            else -> null
        }
    }

    abstract val validUntil: Instant?
    abstract val ott: String?
    abstract val countryCode: String?


    companion object {
        fun createFromStoreEntry(storeEntry: SubjectCredentialStore.StoreEntry): EPrescriptionCredentialAdapter {
            if (storeEntry.scheme !is EPrescriptionScheme) {
                throw IllegalArgumentException("credential")
            }
            return when (storeEntry) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    TODO("Operation not yet supported")
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    EPrescriptionCredentialSdJwtAdapter(
                        storeEntry.toAttributeMap(),
                    )
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    EPrescriptionCredentialIsoMdocAdapter(
                        storeEntry.toNamespaceAttributeMap(),
                    )
                }
            }
        }
    }
}
private class EPrescriptionCredentialSdJwtAdapter(
    attributes: Map<String, Any>,
) : EPrescriptionCredentialAdapter() {
    private val proxy = EPrescriptionCredentialIsoMdocAdapter(
        namespaces = mapOf(EPrescriptionScheme.toString() to attributes),
    )

    override val validUntil: Instant?
        get() = proxy.validUntil

    override val ott: String?
        get() = proxy.ott

    override val countryCode: String?
        get() = proxy.countryCode
}

private class EPrescriptionCredentialIsoMdocAdapter(
    namespaces: Map<String, Map<String, Any>>?,
) : EPrescriptionCredentialAdapter() {
    private val namespace =
        namespaces?.get(EPrescriptionScheme.toString())
            ?: throw IllegalArgumentException("namespaces") // contains required attributes


    override val validUntil: Instant?
        get() = namespace[VALID_UNTIL].toInstantOrNull()

    override val countryCode: String?
        get() = namespace[COUNTRY_CODE] as String?

    override val ott: String?
        get() = namespace[OTT] as String?
}
