package at.asitplus.wallet.app.common

import at.asitplus.dif.InputDescriptor
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.companyregistration.CompanyRegistrationDataElements
import at.asitplus.wallet.companyregistration.CompanyRegistrationScheme
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.healthid.HealthIdScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.data.AttributeIndex
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation.ISO_MDOC
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation.PLAIN_JWT
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation.SD_JWT
import at.asitplus.wallet.lib.data.dif.InputEvaluator
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import at.asitplus.wallet.por.PowerOfRepresentationDataElements
import at.asitplus.wallet.por.PowerOfRepresentationScheme
import at.asitplus.wallet.taxid.TaxIdScheme
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

fun InputDescriptor.extractConsentData(): Triple<CredentialRepresentation, ConstantIndex.CredentialScheme, Map<NormalizedJsonPath, Boolean>> {
    val inputDescriptor = this
    val credentialRepresentation = when {
        inputDescriptor.format?.sdJwt != null || inputDescriptor.format?.jwtSd != null -> SD_JWT
        inputDescriptor.format?.msoMdoc != null -> ISO_MDOC
        else -> PLAIN_JWT
    }
    val pattern = when (credentialRepresentation) {
        SD_JWT -> inputDescriptor.constraints?.fields?.firstOrNull {
            it.path.toString().contains("vct")
        }?.filter?.let {
            it.pattern ?: it.const
        }
        ISO_MDOC -> inputDescriptor.id
        PLAIN_JWT -> throw Throwable("PLAIN_JWT not implemented")
    } ?: throw Throwable("Missing Pattern")

    val scheme = AttributeIndex.schemeSet.firstOrNull {
        when (credentialRepresentation) {
            PLAIN_JWT -> throw Throwable("PLAIN_JWT not implemented")
            // This is not entirely correct, but we'll try to work around incorrect definitions in our credentials
            SD_JWT -> it.sdJwtType == pattern || it.isoNamespace == pattern
            ISO_MDOC -> it.isoDocType == pattern
        }
    } ?: throw Throwable("Missing scheme")

    val constraintsMap = InputEvaluator().evaluateConstraintFieldMatches(
        inputDescriptor = this,
        credential = scheme.toJsonElement(credentialRepresentation),
        pathAuthorizationValidator = { true },
    ).getOrNull() ?: throw Throwable("Unable to evaluate constraints")

    val attributes = constraintsMap.mapNotNull {
        val path = it.value.map { it.normalizedJsonPath }.firstOrNull()
            ?: return@mapNotNull null
        val optional = it.key.optional ?: false
        path to optional
    }.toMap()

    return Triple(credentialRepresentation, scheme, attributes)
}

fun ConstantIndex.CredentialScheme.toJsonElement(
    representation: CredentialRepresentation,
): JsonElement {
    val dataElements = when (this) {
        ConstantIndex.AtomicAttribute2023, IdAustriaScheme, EuPidScheme, MobileDrivingLicenceScheme, HealthIdScheme -> this.claimNames
        // TODO Use: this.claim names for all schemes
        PowerOfRepresentationScheme -> PowerOfRepresentationDataElements.ALL_ELEMENTS
        CertificateOfResidenceScheme -> CertificateOfResidenceDataElements.ALL_ELEMENTS
        CompanyRegistrationScheme -> CompanyRegistrationDataElements.ALL_ELEMENTS
        TaxIdScheme -> TaxIdScheme.ALL_ELEMENTS
        else -> TODO("${this::class.simpleName} not implemented in jsonElementBuilder yet")
    }

    return dataElements.associateWith { "" }.let { attributes ->
        when (representation) {
            PLAIN_JWT -> vckJsonSerializer.encodeToJsonElement(attributes + ("type" to this.vcType))
            SD_JWT -> vckJsonSerializer.encodeToJsonElement(attributes + ("vct" to this.sdJwtType))
            ISO_MDOC -> vckJsonSerializer.encodeToJsonElement(mapOf(this.isoNamespace to attributes))
        }
    }
}