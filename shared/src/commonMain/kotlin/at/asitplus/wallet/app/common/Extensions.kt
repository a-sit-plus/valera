package at.asitplus.wallet.app.common

import at.asitplus.dif.ConstraintFilter
import at.asitplus.dif.InputDescriptor
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.openid.CredentialFormatEnum
import at.asitplus.openid.dcql.DCQLClaimsQueryResult
import at.asitplus.openid.dcql.DCQLCredentialClaimStructure
import at.asitplus.openid.dcql.DCQLCredentialQuery
import at.asitplus.openid.dcql.DCQLCredentialQueryInstance
import at.asitplus.openid.dcql.DCQLCredentialQueryMatchingResult
import at.asitplus.openid.dcql.DCQLIsoMdocCredentialQuery
import at.asitplus.openid.dcql.DCQLSdJwtCredentialQuery
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.jsonpath.core.plus
import at.asitplus.wallet.app.common.thirdParty.kotlinx.serialization.json.normalizedJsonPaths
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
import at.asitplus.wallet.lib.data.dif.PresentationExchangeInputEvaluator
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.oidvci.toFormat
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import at.asitplus.wallet.por.PowerOfRepresentationDataElements
import at.asitplus.wallet.por.PowerOfRepresentationScheme
import at.asitplus.wallet.taxid.TaxIdScheme
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

fun InputDescriptor.extractConsentData(): Triple<CredentialRepresentation, ConstantIndex.CredentialScheme, Map<NormalizedJsonPath, Boolean>> {
    @Suppress("DEPRECATION")
    val credentialRepresentation = when {
        this.format == null -> throw IllegalStateException("Format of input descriptor must be set")
        this.format?.sdJwt != null || this.format?.jwtSd != null -> SD_JWT
        this.format?.msoMdoc != null -> ISO_MDOC
        else -> PLAIN_JWT
    }
    val credentialIdentifiers: Collection<String> = when (credentialRepresentation) {
        PLAIN_JWT -> throw Throwable("PLAIN_JWT not implemented")
        SD_JWT -> vctConstraint()?.filter?.referenceValues()
        ISO_MDOC -> listOf(this.id)
    } ?: throw Throwable("Missing Pattern")

    val scheme = AttributeIndex.schemeSet.firstOrNull {
        it.matchAgainstIdentifier(credentialRepresentation, credentialIdentifiers)
    } ?: throw Throwable("Missing scheme")

    val matchedCredentialIdentifier = when (credentialRepresentation) {
        PLAIN_JWT -> throw Throwable("PLAIN_JWT not implemented")
        SD_JWT -> if (scheme.sdJwtType in credentialIdentifiers) scheme.sdJwtType else scheme.isoNamespace
        ISO_MDOC -> scheme.isoNamespace
    }

    val constraintsMap = PresentationExchangeInputEvaluator.evaluateInputDescriptorAgainstCredential(
        inputDescriptor = this,
        credentialClaimStructure = scheme.toJsonElement(credentialRepresentation),
        credentialFormat = credentialRepresentation.toFormat(),
        credentialScheme = matchedCredentialIdentifier,
        fallbackFormatHolder = this.format,
        pathAuthorizationValidator = { true },
    ).getOrThrow()

    val attributes = constraintsMap.mapNotNull {
        val path = it.value.map { it.normalizedJsonPath }.firstOrNull() ?: return@mapNotNull null
        val optional = it.key.optional ?: false
        path to optional
    }.toMap()

    return Triple(credentialRepresentation, scheme, attributes)
}

private fun ConstantIndex.CredentialScheme.matchAgainstIdentifier(
    representation: CredentialRepresentation,
    identifiers: Collection<String>
) = when (representation) {
    PLAIN_JWT -> throw Throwable("PLAIN_JWT not implemented")
    // This is not entirely correct, but we'll try to work around incorrect definitions in our credentials
    SD_JWT -> sdJwtType in identifiers || isoNamespace in identifiers
    ISO_MDOC -> isoDocType in identifiers
}

private fun InputDescriptor.vctConstraint() = constraints?.fields?.firstOrNull { it.path.toString().contains("vct") }

private fun ConstraintFilter.referenceValues() = (pattern ?: const?.content)?.let { listOf(it) } ?: enum

fun DCQLCredentialQuery.extractConsentData(): Triple<CredentialRepresentation, ConstantIndex.CredentialScheme, List<NormalizedJsonPath>> {
    val representation = when (format) {
        CredentialFormatEnum.VC_SD_JWT,
        CredentialFormatEnum.DC_SD_JWT -> SD_JWT

        CredentialFormatEnum.MSO_MDOC -> ISO_MDOC
        else -> PLAIN_JWT
    }

    val scheme = when (this) {
        is DCQLIsoMdocCredentialQuery -> meta?.doctypeValue?.let { AttributeIndex.resolveIsoDoctype(it) }
        is DCQLSdJwtCredentialQuery -> meta?.vctValues?.let {
            if (it.size == 1) AttributeIndex.resolveSdJwtAttributeType(it.first()) else null
        }

        is DCQLCredentialQueryInstance -> null
    } ?: throw Throwable("Missing scheme")

    val schemeJsonElement = scheme.toJsonElement(representation)

    val match = executeCredentialQueryAgainstCredential(
        credential = scheme,
        credentialFormatExtractor = { format },
        credentialClaimStructureExtractor = {
            when (representation) {
                PLAIN_JWT,
                SD_JWT -> DCQLCredentialClaimStructure.JsonBasedStructure(schemeJsonElement)

                ISO_MDOC -> schemeJsonElement.let {
                    DCQLCredentialClaimStructure.IsoMdocStructure(
                        it.jsonObject.toMap().mapValues {
                            it.value.jsonObject.toMap()
                        }
                    )
                }
            }
        },
        mdocCredentialDoctypeExtractor = {
            scheme.isoDocType ?: throw IllegalArgumentException("Credential is not an MDOC")
        },
        sdJwtCredentialTypeExtractor = {
            scheme.sdJwtType ?: throw IllegalArgumentException("Credential is not an SD-JWT")
        },
    ).getOrNull() ?: throw Throwable("Unable to evaluate credential matching result.")

    val normalizedJsonPaths = when (match) {
        DCQLCredentialQueryMatchingResult.AllClaimsMatchingResult -> schemeJsonElement.normalizedJsonPaths()

        is DCQLCredentialQueryMatchingResult.ClaimsQueryResults -> match.claimsQueryResults.flatMap {
            when (it) {
                is DCQLClaimsQueryResult.IsoMdocResult -> listOf(NormalizedJsonPath() + it.namespace + it.claimName)
                is DCQLClaimsQueryResult.JsonResult -> it.nodeList.map {
                    it.normalizedJsonPath
                }
            }
        }
    }
    return Triple(representation, scheme, normalizedJsonPaths)
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
