package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.attribute_friendly_name_age_at_least_12
import at.asitplus.valera.resources.attribute_friendly_name_age_at_least_13
import at.asitplus.valera.resources.attribute_friendly_name_age_at_least_14
import at.asitplus.valera.resources.attribute_friendly_name_age_at_least_16
import at.asitplus.valera.resources.attribute_friendly_name_age_at_least_18
import at.asitplus.valera.resources.attribute_friendly_name_age_at_least_21
import at.asitplus.valera.resources.attribute_friendly_name_age_at_least_25
import at.asitplus.valera.resources.attribute_friendly_name_age_at_least_60
import at.asitplus.valera.resources.attribute_friendly_name_age_at_least_62
import at.asitplus.valera.resources.attribute_friendly_name_age_at_least_65
import at.asitplus.valera.resources.attribute_friendly_name_age_at_least_68
import at.asitplus.wallet.ageverification.AgeVerificationScheme
import org.jetbrains.compose.resources.StringResource


class AgeVerificationCredentialAttributeTranslator : CredentialAttributeTranslator {
    override fun translate(attributeName: NormalizedJsonPath): StringResource? = listOfNotNull(
        attributeName.segments.getOrNull(1),
        attributeName.segments.firstOrNull(),
    ).filterIsInstance<NormalizedJsonPathSegment.NameSegment>().firstNotNullOfOrNull {
        AgeVerificationCredentialMdocClaimDefinitionResolver().resolveOrNull(
            namespace = AgeVerificationScheme.isoNamespace,
            claimName = it.memberName
        )
    }?.stringResourceOrNull()

    private fun AgeVerificationCredentialClaimDefinition.stringResourceOrNull(): StringResource? = when (this) {
        AgeVerificationCredentialClaimDefinition.AGE_OVER_12 -> Res.string.attribute_friendly_name_age_at_least_12
        AgeVerificationCredentialClaimDefinition.AGE_OVER_13 -> Res.string.attribute_friendly_name_age_at_least_13
        AgeVerificationCredentialClaimDefinition.AGE_OVER_14 -> Res.string.attribute_friendly_name_age_at_least_14
        AgeVerificationCredentialClaimDefinition.AGE_OVER_16 -> Res.string.attribute_friendly_name_age_at_least_16
        AgeVerificationCredentialClaimDefinition.AGE_OVER_18 -> Res.string.attribute_friendly_name_age_at_least_18
        AgeVerificationCredentialClaimDefinition.AGE_OVER_21 -> Res.string.attribute_friendly_name_age_at_least_21
        AgeVerificationCredentialClaimDefinition.AGE_OVER_25 -> Res.string.attribute_friendly_name_age_at_least_25
        AgeVerificationCredentialClaimDefinition.AGE_OVER_60 -> Res.string.attribute_friendly_name_age_at_least_60
        AgeVerificationCredentialClaimDefinition.AGE_OVER_62 -> Res.string.attribute_friendly_name_age_at_least_62
        AgeVerificationCredentialClaimDefinition.AGE_OVER_65 -> Res.string.attribute_friendly_name_age_at_least_65
        AgeVerificationCredentialClaimDefinition.AGE_OVER_68 -> Res.string.attribute_friendly_name_age_at_least_68

    }
}

