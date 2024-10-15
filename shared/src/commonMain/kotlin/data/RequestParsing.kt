package at.asitplus.misc

import at.asitplus.dif.ConstraintField
import at.asitplus.dif.InputDescriptor
import at.asitplus.jsonpath.core.NodeList
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import at.asitplus.wallet.eprescription.EPrescriptionDataElements
import at.asitplus.wallet.eprescription.EPrescriptionScheme
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.data.AttributeIndex
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.dif.InputEvaluator
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import at.asitplus.wallet.por.PowerOfRepresentationDataElements
import at.asitplus.wallet.por.PowerOfRepresentationScheme
import data.RequestOptionParameters
import io.github.aakira.napier.Napier
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement


data class CredentialCandidate(
    val scheme: ConstantIndex.CredentialScheme,
    val representation: ConstantIndex.CredentialRepresentation,
    val jsonElement: JsonElement,
)

val allCredentialCandidates: List<CredentialCandidate> = AttributeIndex.schemeSet.flatMap { scheme ->
    scheme.supportedRepresentations.map { representation ->
        CredentialCandidate(
            scheme = scheme, representation = representation, jsonElement = scheme.toJsonElement(representation)
        )
    }
}

fun InputDescriptor.getRequestOptionParameters(): RequestOptionParameters? {
    val (candidate, result) = this.evaluateAgainstKnownSchemes { true }
    return result.toRequestOptionParameters(candidate.scheme, candidate.representation)
}

private fun InputDescriptor.evaluateAgainstKnownSchemes(
    pathAuthorizationValidator: (NormalizedJsonPath) -> Boolean,
): Pair<CredentialCandidate, Map<ConstraintField, NodeList>> {
    val viableCandidates = filterCredentialCandidates(this.id)

    return viableCandidates.firstNotNullOfOrNull { candidate ->
        InputEvaluator().evaluateConstraintFieldMatches(
            inputDescriptor = this,
            credential = candidate.jsonElement,
            pathAuthorizationValidator = pathAuthorizationValidator,
        ).getOrNull()?.let { candidate to it }
    } ?: throw Exception(
        "No known Scheme fulfills the request for $this."
    )
}

private fun filterCredentialCandidates(identifier: String): List<CredentialCandidate> {
    val credentialIdentifiers = AttributeIndex.resolveCredential(identifier)
    val viableCandidates = allCredentialCandidates.toMutableList()

    credentialIdentifiers?.let { (scheme, representation) ->
        // Filter viableCandidates to include only those matching the scheme and representation
        viableCandidates.retainAll { candidate ->
            candidate.scheme == scheme
        }
        representation?.let {
            viableCandidates.retainAll { candidate -> candidate.representation == it }
        }
    }

    return viableCandidates
}


private fun Map<ConstraintField, NodeList>.toRequestOptionParameters(
    scheme: ConstantIndex.CredentialScheme,
    representation: ConstantIndex.CredentialRepresentation,
): RequestOptionParameters? {
    val attributeMap = this.values.flatten().associate { value ->
        value.normalizedJsonPath.segments.filterIsInstance<NormalizedJsonPathSegment.NameSegment>()
            .last().memberName to value.value.toString()
    }

    val schemeIdentifier = when (representation) {
        ConstantIndex.CredentialRepresentation.PLAIN_JWT -> (attributeMap["type"]
            ?: null.also { Napier.d("$representation does not have attribute \".type\"?") })

        ConstantIndex.CredentialRepresentation.SD_JWT -> (attributeMap["vct"]
            ?: null.also { Napier.d("$representation does not have attribute \".vct\"?") })

        // Assume that only IsoDocType does not have a special constraint
        else -> scheme.isoDocType ?: null.also { Napier.d("$scheme does not have IsoDocType") }
    }
    //if no identifier was found return null because it cannot be processed
    return schemeIdentifier?.let {
        RequestOptionParameters(
            it.removeSurrounding("\""), attributeMap.keys.filter { it != "type" && it != "vct" }.toSet()
        )
    }
}

private fun ConstantIndex.CredentialScheme.toJsonElement(
    representation: ConstantIndex.CredentialRepresentation,
): JsonElement {
    val dataElements = when (this) {
        ConstantIndex.AtomicAttribute2023, IdAustriaScheme, EuPidScheme, MobileDrivingLicenceScheme -> this.claimNames
        PowerOfRepresentationScheme -> PowerOfRepresentationDataElements.ALL_ELEMENTS
        CertificateOfResidenceScheme -> CertificateOfResidenceDataElements.ALL_ELEMENTS
        EPrescriptionScheme -> EPrescriptionDataElements.ALL_ELEMENTS
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