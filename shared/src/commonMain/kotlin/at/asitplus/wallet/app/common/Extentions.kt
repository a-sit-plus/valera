package at.asitplus.wallet.app.common

import at.asitplus.dif.InputDescriptor
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.openid.third_party.at.asitplus.jsonpath.core.plus
import at.asitplus.wallet.companyregistration.CompanyRegistrationDataElements
import at.asitplus.wallet.companyregistration.CompanyRegistrationScheme
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.dif.InputEvaluator
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import at.asitplus.wallet.por.PowerOfRepresentationScheme
import at.asitplus.wallet.taxid.TaxIdScheme
import at.asitplus.wallet.healthid.HealthIdScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.AttributeIndex
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.por.PowerOfRepresentationDataElements
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

fun InputDescriptor.extractConsentData(): Triple<ConstantIndex.CredentialRepresentation, ConstantIndex.CredentialScheme, Map<NormalizedJsonPath, Boolean>> {
    val inputDescriptor = this
    val credentialRepresentation = if (inputDescriptor.format?.sdJwt != null) {
        ConstantIndex.CredentialRepresentation.SD_JWT
    } else if (inputDescriptor.format?.msoMdoc != null) {
        ConstantIndex.CredentialRepresentation.ISO_MDOC
    } else {
        ConstantIndex.CredentialRepresentation.PLAIN_JWT
    }
    val pattern = when (credentialRepresentation) {
        ConstantIndex.CredentialRepresentation.SD_JWT -> inputDescriptor.constraints?.fields?.last()?.filter?.pattern
        ConstantIndex.CredentialRepresentation.ISO_MDOC -> inputDescriptor.id
        ConstantIndex.CredentialRepresentation.PLAIN_JWT -> throw Throwable("PLAIN_JWT not implemented")
    } ?: throw Throwable("Missing Pattern")

    val scheme = AttributeIndex.schemeSet.firstOrNull {
        when (credentialRepresentation) {
            ConstantIndex.CredentialRepresentation.PLAIN_JWT -> throw Throwable("PLAIN_JWT not implemented")
            ConstantIndex.CredentialRepresentation.SD_JWT -> it.sdJwtType == pattern
            ConstantIndex.CredentialRepresentation.ISO_MDOC -> it.isoDocType == pattern
        }
    } ?: throw Throwable("Missing scheme")

    val constraintsMap = InputEvaluator().evaluateConstraintFieldMatches(
        inputDescriptor = this,
        credential = scheme.toJsonElement(credentialRepresentation),
        pathAuthorizationValidator = { true },
    ).getOrNull() ?: throw Throwable("Unable to evaluate constraints")

    val attributes = constraintsMap.mapNotNull {
        val path = it.value.map { value ->
            value.normalizedJsonPath
        }.firstOrNull() ?: return@mapNotNull null
        val optional = it.key.optional ?: return@mapNotNull null
        path to optional
    }.toMap()

    return Triple(credentialRepresentation, scheme, attributes)
}

fun ConstantIndex.CredentialScheme.toJsonElement(
    representation: ConstantIndex.CredentialRepresentation,
): JsonElement {
    val dataElements = when (this) {
        ConstantIndex.AtomicAttribute2023, IdAustriaScheme, EuPidScheme, MobileDrivingLicenceScheme, HealthIdScheme -> this.claimNames
        PowerOfRepresentationScheme -> PowerOfRepresentationDataElements.ALL_ELEMENTS
        CertificateOfResidenceScheme -> CertificateOfResidenceDataElements.ALL_ELEMENTS
        CompanyRegistrationScheme -> CompanyRegistrationDataElements.ALL_ELEMENTS
        TaxIdScheme -> TaxIdScheme.ALL_ELEMENTS
        else -> TODO("${this::class.simpleName} not implemented in jsonElementBuilder yet")
    }

    return dataElements.associateWith { "" }.let { attributes ->
        if (representation != ConstantIndex.CredentialRepresentation.ISO_MDOC) {
            val representationConstraint = when (representation) {
                ConstantIndex.CredentialRepresentation.PLAIN_JWT -> "type" to this.vcType
                ConstantIndex.CredentialRepresentation.SD_JWT -> "vct" to this.sdJwtType
                ConstantIndex.CredentialRepresentation.ISO_MDOC -> "" to this.isoDocType //case can never happen
            }
            vckJsonSerializer.encodeToJsonElement(attributes + representationConstraint)
        } else {
            vckJsonSerializer.encodeToJsonElement(mapOf(this.isoNamespace to attributes))
        }
    }
}

fun SubjectCredentialStore.StoreEntry.getAttributes(): Map<NormalizedJsonPath, String> = when (val credential = this) {
    is SubjectCredentialStore.StoreEntry.Vc -> {
        throw Throwable("Not implemented")
    }

    is SubjectCredentialStore.StoreEntry.SdJwt -> {
        credential.disclosures.entries.associate {
            val claimValue = it.value?.claimValue?.toString() ?: ""
            val claimName = it.value?.claimName ?: ""
            NormalizedJsonPath().plus(claimName) to claimValue
        }
    }

    is SubjectCredentialStore.StoreEntry.Iso -> {
        val list: MutableList<Pair<NormalizedJsonPath, String>> = mutableListOf()
        credential.issuerSigned.namespaces?.forEach { namespace ->
            namespace.value.entries.sortedBy { it.value.elementIdentifier }.forEach { entry ->
                val claimValue = entry.value.elementValue.prettyToString()
                val claimName = NormalizedJsonPath(
                    NormalizedJsonPathSegment.NameSegment(namespace.key),
                    NormalizedJsonPathSegment.NameSegment(entry.value.elementIdentifier),
                )
                list.add(Pair(claimName, claimValue))
            }
        }
        list.toMap()
    }
}

private fun Any.prettyToString() = when (this) {
    is Array<*> -> contentToString()
    else -> toString()
}