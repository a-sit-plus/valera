package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.ageverification.AgeVerificationScheme
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation
import data.Attribute

sealed class AgeVerificationCredentialAdapter(
) : CredentialAdapter() {

    override fun getAttribute(path: NormalizedJsonPath) = listOfNotNull(
        path.segments.getOrNull(1),
        path.segments.firstOrNull(),
    ).filterIsInstance<NormalizedJsonPathSegment.NameSegment>().firstNotNullOfOrNull {
        AgeVerificationCredentialMdocClaimDefinitionResolver().resolveOrNull(
            namespace = AgeVerificationScheme.isoNamespace,
            claimName = it.memberName
        )?.toAttribute()
    }

    abstract val ageAtLeast12: Boolean?
    abstract val ageAtLeast13: Boolean?
    abstract val ageAtLeast14: Boolean?
    abstract val ageAtLeast16: Boolean?
    abstract val ageAtLeast18: Boolean?
    abstract val ageAtLeast21: Boolean?
    abstract val ageAtLeast25: Boolean?
    abstract val ageAtLeast60: Boolean?
    abstract val ageAtLeast62: Boolean?
    abstract val ageAtLeast65: Boolean?
    abstract val ageAtLeast68: Boolean?

    companion object {
        fun createFromStoreEntry(
            storeEntry: SubjectCredentialStore.StoreEntry,
        ): AgeVerificationCredentialAdapter {
            if (storeEntry.scheme !is AgeVerificationScheme) {
                throw IllegalArgumentException("credential")
            }
            return when (storeEntry) {
                is SubjectCredentialStore.StoreEntry.Iso ->
                    AgeVerificationCredentialIsoMdocAdapter(storeEntry.toNamespaceAttributeMap())

                else -> TODO("Operation not yet supported")
            }
        }
    }

    private fun AgeVerificationCredentialClaimDefinition.toAttribute(): Attribute? = when (this) {
        AgeVerificationCredentialClaimDefinition.AGE_OVER_12 -> Attribute.Companion.fromValue(ageAtLeast12)
        AgeVerificationCredentialClaimDefinition.AGE_OVER_13 -> Attribute.Companion.fromValue(ageAtLeast13)
        AgeVerificationCredentialClaimDefinition.AGE_OVER_14 -> Attribute.Companion.fromValue(ageAtLeast14)
        AgeVerificationCredentialClaimDefinition.AGE_OVER_16 -> Attribute.Companion.fromValue(ageAtLeast16)
        AgeVerificationCredentialClaimDefinition.AGE_OVER_18 -> Attribute.Companion.fromValue(ageAtLeast18)
        AgeVerificationCredentialClaimDefinition.AGE_OVER_21 -> Attribute.Companion.fromValue(ageAtLeast21)
        AgeVerificationCredentialClaimDefinition.AGE_OVER_25 -> Attribute.Companion.fromValue(ageAtLeast25)
        AgeVerificationCredentialClaimDefinition.AGE_OVER_60 -> Attribute.Companion.fromValue(ageAtLeast60)
        AgeVerificationCredentialClaimDefinition.AGE_OVER_62 -> Attribute.Companion.fromValue(ageAtLeast62)
        AgeVerificationCredentialClaimDefinition.AGE_OVER_65 -> Attribute.Companion.fromValue(ageAtLeast65)
        AgeVerificationCredentialClaimDefinition.AGE_OVER_68 -> Attribute.Companion.fromValue(ageAtLeast68)
    }
}

class AgeVerificationCredentialIsoMdocAdapter(
    namespaces: Map<String, Map<String, Any>>?,
) : AgeVerificationCredentialAdapter() {
    override val scheme: ConstantIndex.CredentialScheme
        get() = AgeVerificationScheme

    private val namespace = namespaces?.get(AgeVerificationScheme.isoNamespace)

    override val representation: CredentialRepresentation
        get() = CredentialRepresentation.ISO_MDOC


    override val ageAtLeast12: Boolean?
        get() = namespace?.get(AgeVerificationScheme.Attributes.AGE_OVER_12)?.toString()?.toBooleanStrictOrNull()

    override val ageAtLeast13: Boolean?
        get() = namespace?.get(AgeVerificationScheme.Attributes.AGE_OVER_13)?.toString()?.toBooleanStrictOrNull()

    override val ageAtLeast14: Boolean?
        get() = namespace?.get(AgeVerificationScheme.Attributes.AGE_OVER_14)?.toString()?.toBooleanStrictOrNull()

    override val ageAtLeast16: Boolean?
        get() = namespace?.get(AgeVerificationScheme.Attributes.AGE_OVER_16)?.toString()?.toBooleanStrictOrNull()

    override val ageAtLeast18: Boolean?
        get() = namespace?.get(AgeVerificationScheme.Attributes.AGE_OVER_18)?.toString()?.toBooleanStrictOrNull()

    override val ageAtLeast21: Boolean?
        get() = namespace?.get(AgeVerificationScheme.Attributes.AGE_OVER_21)?.toString()?.toBooleanStrictOrNull()

    override val ageAtLeast25: Boolean?
        get() = namespace?.get(AgeVerificationScheme.Attributes.AGE_OVER_25)?.toString()?.toBooleanStrictOrNull()

    override val ageAtLeast60: Boolean?
        get() = namespace?.get(AgeVerificationScheme.Attributes.AGE_OVER_60)?.toString()?.toBooleanStrictOrNull()

    override val ageAtLeast62: Boolean?
        get() = namespace?.get(AgeVerificationScheme.Attributes.AGE_OVER_62)?.toString()?.toBooleanStrictOrNull()

    override val ageAtLeast65: Boolean?
        get() = namespace?.get(AgeVerificationScheme.Attributes.AGE_OVER_65)?.toString()?.toBooleanStrictOrNull()

    override val ageAtLeast68: Boolean?
        get() = namespace?.get(AgeVerificationScheme.Attributes.AGE_OVER_68)?.toString()?.toBooleanStrictOrNull()

}
