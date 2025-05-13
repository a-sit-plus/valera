package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements.DOCUMENT_NUMBER
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements.EXPIRY_DATE
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements.ISSUANCE_DATE
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements.ISSUING_AUTHORITY
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements.ISSUING_COUNTRY
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import at.asitplus.wallet.ehic.EhicScheme
import at.asitplus.wallet.ehic.EhicScheme.Attributes.ISSUING_AUTHORITY_ID
import at.asitplus.wallet.ehic.EhicScheme.Attributes.ISSUING_AUTHORITY_NAME
import at.asitplus.wallet.ehic.EhicScheme.Attributes.IssuingAuthority.ID
import at.asitplus.wallet.ehic.EhicScheme.Attributes.IssuingAuthority.NAME
import at.asitplus.wallet.ehic.EhicScheme.Attributes.SOCIAL_SECURITY_NUMBER
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation
import data.Attribute
import kotlinx.datetime.Instant
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

sealed class EhicCredentialAdapter : CredentialAdapter() {
    override fun getAttribute(path: NormalizedJsonPath) =
        path.segments.firstOrNull()?.let { first ->
            with(EhicScheme.Attributes) {
                when (first) {
                    is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
                        ISSUING_COUNTRY -> Attribute.fromValue(issuingCountry)
                        SOCIAL_SECURITY_NUMBER -> Attribute.fromValue(socialSecurityNumber)
                        PREFIX_ISSUING_AUTHORITY -> when (val second =
                            path.segments.drop(1).firstOrNull()) {
                            is NormalizedJsonPathSegment.NameSegment -> when (second.memberName) {
                                ID -> Attribute.fromValue(issuingAuthorityId)
                                NAME -> Attribute.fromValue(issuingAuthorityName)
                                else -> null
                            }

                            else -> Attribute.fromValue(issuingAuthority)
                        }

                        ISSUING_AUTHORITY_ID -> Attribute.fromValue(issuingAuthorityId)
                        ISSUING_AUTHORITY_NAME -> Attribute.fromValue(issuingAuthorityName)

                        DOCUMENT_NUMBER -> Attribute.fromValue(documentNumber)
                        ISSUANCE_DATE -> Attribute.fromValue(issuanceDate)
                        EXPIRY_DATE -> Attribute.fromValue(expiryDate)
                        else -> null
                    }

                    else -> null
                }
            }
        }

    abstract val issuingCountry: String?
    abstract val socialSecurityNumber: String?
    abstract val issuingAuthority: String?
    abstract val issuingAuthorityId: String?
    abstract val issuingAuthorityName: String?
    abstract val documentNumber: String?
    abstract val issuanceDate: Instant?
    abstract val expiryDate: Instant?

    companion object {
        fun createFromStoreEntry(storeEntry: SubjectCredentialStore.StoreEntry): EhicCredentialAdapter {
            if (storeEntry.scheme !is EhicScheme) {
                throw IllegalArgumentException("credential")
            }
            return when (storeEntry) {
                is SubjectCredentialStore.StoreEntry.SdJwt -> storeEntry.toComplexJson()
                    ?.let { EhicComplexCredentialSdJwtAdapter(it) } ?: EhicCredentialSdJwtAdapter(
                    storeEntry.toAttributeMap()
                )

                else -> TODO("Operation not yet supported")
            }
        }
    }
}

private class EhicCredentialSdJwtAdapter(
    private val attributes: Map<String, JsonPrimitive>,
) : EhicCredentialAdapter() {
    override val scheme: ConstantIndex.CredentialScheme
        get() = CertificateOfResidenceScheme

    override val representation: CredentialRepresentation
        get() = CredentialRepresentation.SD_JWT

    override val documentNumber: String?
        get() = attributes[DOCUMENT_NUMBER]?.contentOrNull

    override val issuingAuthority: String?
        get() = attributes[ISSUING_AUTHORITY]?.contentOrNull

    override val issuingAuthorityId: String?
        get() = attributes[ID]?.contentOrNull

    override val issuingAuthorityName: String?
        get() = attributes[NAME]?.contentOrNull

    override val socialSecurityNumber: String?
        get() = attributes[SOCIAL_SECURITY_NUMBER]?.contentOrNull

    override val issuanceDate: Instant?
        get() = attributes[ISSUANCE_DATE]?.contentOrNull?.toInstantOrNull()

    override val expiryDate: Instant?
        get() = attributes[EXPIRY_DATE]?.contentOrNull?.toInstantOrNull()

    override val issuingCountry: String?
        get() = attributes[ISSUING_COUNTRY]?.contentOrNull
}


private class EhicComplexCredentialSdJwtAdapter(
    private val attributes: JsonObject,
) : EhicCredentialAdapter() {
    override val scheme: ConstantIndex.CredentialScheme
        get() = CertificateOfResidenceScheme

    override val representation: CredentialRepresentation
        get() = CredentialRepresentation.SD_JWT

    override val documentNumber: String?
        get() = (attributes[DOCUMENT_NUMBER] as? JsonPrimitive?)?.contentOrNull

    override val issuingAuthority: String?
        get() = (attributes[ISSUING_AUTHORITY] as? JsonPrimitive?)?.contentOrNull

    override val issuingAuthorityId: String?
        get() = (attributes[ISSUING_AUTHORITY] as? JsonObject?)?.let {
            (it[ID] as? JsonPrimitive?)?.contentOrNull
        } ?: (attributes[ISSUING_AUTHORITY_ID] as? JsonPrimitive?)?.contentOrNull

    override val issuingAuthorityName: String?
        get() = (attributes[ISSUING_AUTHORITY] as? JsonObject?)?.let {
            (it[NAME] as? JsonPrimitive?)?.contentOrNull
        } ?: (attributes[ISSUING_AUTHORITY_NAME] as? JsonPrimitive?)?.contentOrNull

    override val socialSecurityNumber: String?
        get() = (attributes[SOCIAL_SECURITY_NUMBER] as JsonPrimitive?)?.contentOrNull

    override val issuanceDate: Instant?
        get() = (attributes[ISSUANCE_DATE] as? JsonPrimitive?)?.contentOrNull?.toInstantOrNull()

    override val expiryDate: Instant?
        get() = (attributes[EXPIRY_DATE] as? JsonPrimitive?)?.contentOrNull?.toInstantOrNull()

    override val issuingCountry: String?
        get() = (attributes[ISSUING_COUNTRY] as? JsonPrimitive?)?.contentOrNull
}
