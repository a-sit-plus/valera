package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.ageverification.AgeVerificationScheme
import data.PersonalDataCategory

object AgeVerificationCredentialAttributeCategorization : CredentialAttributeCategorization.Template(
    mapOf(
        PersonalDataCategory.AgeData to listOf(
            AgeVerificationScheme.Attributes.AGE_OVER_12,
            AgeVerificationScheme.Attributes.AGE_OVER_13,
            AgeVerificationScheme.Attributes.AGE_OVER_14,
            AgeVerificationScheme.Attributes.AGE_OVER_16,
            AgeVerificationScheme.Attributes.AGE_OVER_18,
            AgeVerificationScheme.Attributes.AGE_OVER_21,
            AgeVerificationScheme.Attributes.AGE_OVER_25,
            AgeVerificationScheme.Attributes.AGE_OVER_60,
            AgeVerificationScheme.Attributes.AGE_OVER_62,
            AgeVerificationScheme.Attributes.AGE_OVER_65,
            AgeVerificationScheme.Attributes.AGE_OVER_68,
        ).map { NormalizedJsonPath() + it to null },
    ),
    allAttributes = AgeVerificationScheme.claimNames.map {
        NormalizedJsonPath() + it
    },
)
