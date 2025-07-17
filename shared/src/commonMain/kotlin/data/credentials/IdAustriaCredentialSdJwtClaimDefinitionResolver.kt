package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.idaustria.IdAustriaScheme

class IdAustriaCredentialSdJwtClaimDefinitionResolver {
    fun resolveOrNull(attributeName: NormalizedJsonPath) =
        when (val first = attributeName.segments.firstOrNull()) {
            is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
                IdAustriaScheme.Attributes.BPK -> IdAustriaCredentialClaimDefinition.BPK
                IdAustriaScheme.Attributes.FIRSTNAME -> IdAustriaCredentialClaimDefinition.FIRSTNAME
                IdAustriaScheme.Attributes.LASTNAME -> IdAustriaCredentialClaimDefinition.LASTNAME
                IdAustriaScheme.Attributes.DATE_OF_BIRTH -> IdAustriaCredentialClaimDefinition.DATE_OF_BIRTH
                IdAustriaScheme.Attributes.PORTRAIT -> IdAustriaCredentialClaimDefinition.PORTRAIT
                IdAustriaScheme.Attributes.AGE_OVER_14 -> IdAustriaCredentialClaimDefinition.AGE_OVER_14
                IdAustriaScheme.Attributes.AGE_OVER_16 -> IdAustriaCredentialClaimDefinition.AGE_OVER_16
                IdAustriaScheme.Attributes.AGE_OVER_18 -> IdAustriaCredentialClaimDefinition.AGE_OVER_18
                IdAustriaScheme.Attributes.AGE_OVER_21 -> IdAustriaCredentialClaimDefinition.AGE_OVER_21
                IdAustriaScheme.Attributes.MAIN_ADDRESS -> {
                    when (val second = attributeName.segments.getOrNull(1)) {
                        is NormalizedJsonPathSegment.NameSegment -> when (second.memberName) {
                            IdAustriaCredentialMainAddress.GEMEINDEKENNZIFFER -> IdAustriaCredentialClaimDefinition.MAIN_ADDRESS_GEMEINDEKENNZIFFER
                            IdAustriaCredentialMainAddress.GEMEINDEBEZEICHNUNG -> IdAustriaCredentialClaimDefinition.MAIN_ADDRESS_GEMEINDEBEZEICHNUNG
                            IdAustriaCredentialMainAddress.POSTLEITZAHL -> IdAustriaCredentialClaimDefinition.MAIN_ADDRESS_POSTLEITZAHL
                            IdAustriaCredentialMainAddress.ORTSCHAFT -> IdAustriaCredentialClaimDefinition.MAIN_ADDRESS_ORTSCHAFT
                            IdAustriaCredentialMainAddress.STRASSE -> IdAustriaCredentialClaimDefinition.MAIN_ADDRESS_STRASSE
                            IdAustriaCredentialMainAddress.HAUSNUMMER -> IdAustriaCredentialClaimDefinition.MAIN_ADDRESS_HAUSNUMMER
                            IdAustriaCredentialMainAddress.STIEGE -> IdAustriaCredentialClaimDefinition.MAIN_ADDRESS_STIEGE
                            IdAustriaCredentialMainAddress.TUER -> IdAustriaCredentialClaimDefinition.MAIN_ADDRESS_TUER
                            else -> null
                        }

                        null -> IdAustriaCredentialClaimDefinition.MAIN_ADDRESS_CONTAINER
                        else -> null
                    }
                }

                else -> null
            }

            else -> null
        }
}