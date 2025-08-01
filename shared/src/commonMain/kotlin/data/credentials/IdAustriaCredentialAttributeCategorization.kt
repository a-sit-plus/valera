package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment.NameSegment
import at.asitplus.wallet.idaustria.IdAustriaScheme
import data.PersonalDataCategory

object IdAustriaCredentialAttributeCategorization : CredentialAttributeCategorization.Template(
    mapOf(
        PersonalDataCategory.IdentityData to listOf(
            IdAustriaScheme.Attributes.FIRSTNAME,
            IdAustriaScheme.Attributes.LASTNAME,
            IdAustriaScheme.Attributes.DATE_OF_BIRTH,
            IdAustriaScheme.Attributes.PORTRAIT,
        ).map { NormalizedJsonPath() + it to null },

        PersonalDataCategory.AgeData to listOf(
            IdAustriaScheme.Attributes.AGE_OVER_14,
            IdAustriaScheme.Attributes.AGE_OVER_16,
            IdAustriaScheme.Attributes.AGE_OVER_18,
            IdAustriaScheme.Attributes.AGE_OVER_21,
        ).map { NormalizedJsonPath() + it to null },

        PersonalDataCategory.ResidenceData to listOf(
            (NormalizedJsonPath() + IdAustriaScheme.Attributes.MAIN_ADDRESS) to listOf(
                IdAustriaCredentialMainAddress.STRASSE,
                IdAustriaCredentialMainAddress.HAUSNUMMER,
                IdAustriaCredentialMainAddress.STIEGE,
                IdAustriaCredentialMainAddress.TUER,
                IdAustriaCredentialMainAddress.POSTLEITZAHL,
                IdAustriaCredentialMainAddress.ORTSCHAFT,
                IdAustriaCredentialMainAddress.GEMEINDEBEZEICHNUNG,
                IdAustriaCredentialMainAddress.GEMEINDEKENNZIFFER,
            ).map { NormalizedJsonPath() + it },
        ),

        PersonalDataCategory.AdmissionData to listOf(
            IdAustriaScheme.Attributes.VEHICLE_REGISTRATION, // TODO: Extract data
        ).map { NormalizedJsonPath() + it to null },
    ),
    allAttributes = IdAustriaScheme.claimNames.map {
        NormalizedJsonPath(NameSegment(it))
    },
)