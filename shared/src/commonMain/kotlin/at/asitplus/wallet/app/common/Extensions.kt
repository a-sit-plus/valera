package at.asitplus.wallet.app.common

import androidx.compose.runtime.Composable
import at.asitplus.dif.Constraint
import at.asitplus.catchingUnwrapped
import at.asitplus.dif.ConstraintField
import at.asitplus.dif.ConstraintFilter
import at.asitplus.dif.DifInputDescriptor
import at.asitplus.dif.FormatContainerJwt
import at.asitplus.dif.FormatHolder
import at.asitplus.dif.InputDescriptor
import at.asitplus.iso.DocRequest
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment.*
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment.NameSegment
import at.asitplus.openid.CredentialFormatEnum
import at.asitplus.openid.dcql.*
import at.asitplus.openid.dcql.DCQLJwtVcCredentialQuery
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.getLocalization
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.uiLabel
import at.asitplus.wallet.companyregistration.CompanyRegistrationDataElements
import at.asitplus.wallet.companyregistration.CompanyRegistrationScheme
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import at.asitplus.wallet.ehic.EhicScheme
import at.asitplus.wallet.eupidsdjwt.EuPidSdJwtScheme
import at.asitplus.wallet.lib.data.AttributeIndex
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation.*
import at.asitplus.wallet.lib.data.IsoMdocFallbackCredentialScheme
import at.asitplus.wallet.lib.data.SdJwtFallbackCredentialScheme
import at.asitplus.wallet.lib.data.VcDataModelConstants.VERIFIABLE_CREDENTIAL
import at.asitplus.wallet.lib.data.VcFallbackCredentialScheme
import at.asitplus.wallet.lib.data.dif.ConstraintFieldsEvaluationException
import at.asitplus.wallet.lib.data.dif.PresentationExchangeInputEvaluator
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.oidvci.toFormat
import data.credentials.JsonClaimReference
import data.credentials.JwtClaimDefinition
import data.credentials.JwtClaimDefinitionTranslator
import data.credentials.MdocClaimReference
import data.credentials.SingleClaimReference
import kotlinx.serialization.json.*
import org.jetbrains.compose.resources.stringResource
import ui.presentation.DCQLCredentialQueryUiModel
import ui.presentation.DCQLCredentialQueryUiModelAttributeLabels

fun InputDescriptor.extractConsentData(): Triple<CredentialRepresentation, ConstantIndex.CredentialScheme, Map<NormalizedJsonPath, Boolean>> {
    @Suppress("DEPRECATION")
    val credentialRepresentation = when {
        this.format == null -> throw IllegalStateException("Format of input descriptor must be set")
        this.format?.sdJwt != null -> SD_JWT
        this.format?.msoMdoc != null -> ISO_MDOC
        else -> PLAIN_JWT
    }
    val credentialIdentifiers = when (credentialRepresentation) {
        PLAIN_JWT -> throw Throwable("PLAIN_JWT not implemented")
        SD_JWT -> vctConstraint()?.filter?.referenceValues()
        ISO_MDOC -> listOf(this.id)
    } ?: throw Throwable("Missing Pattern")

    // TODO: How to properly handle the case with multiple applicable schemes?
    val scheme = AttributeIndex.schemeSet.firstOrNull {
        it.matchAgainstIdentifier(credentialRepresentation, credentialIdentifiers)
    } ?: when (credentialRepresentation) {
        PLAIN_JWT -> VcFallbackCredentialScheme(vcType = credentialIdentifiers.first())
        SD_JWT -> SdJwtFallbackCredentialScheme(sdJwtType = credentialIdentifiers.first())
        ISO_MDOC -> IsoMdocFallbackCredentialScheme(isoDocType = credentialIdentifiers.first())
    }

    val matchedCredentialIdentifier = when (credentialRepresentation) {
        PLAIN_JWT -> throw Throwable("PLAIN_JWT not implemented")
        SD_JWT -> if (scheme.sdJwtType in credentialIdentifiers) scheme.sdJwtType else scheme.isoNamespace
        ISO_MDOC -> scheme.isoDocType
    }

    val requestedElements = constraints?.fields?.map {
        (it.toNormalizedJsonPath()?.segments?.last() as NameSegment).memberName
    }

    val constraintsMap = PresentationExchangeInputEvaluator.evaluateInputDescriptorAgainstCredential(
        inputDescriptor = this,
        credentialClaimStructure = scheme.toJsonElement(credentialRepresentation, requestedElements),
        credentialFormat = credentialRepresentation.toFormat(),
        credentialScheme = matchedCredentialIdentifier,
        fallbackFormatHolder = this.format,
        pathAuthorizationValidator = { true },
    ).getOrThrow()

    val attributes = constraintsMap.mapNotNull {
        val path = it.value.map { it.normalizedJsonPath }.firstOrNull() ?: return@mapNotNull null
        val optional = it.key.optional == true
        path to optional
    }.toMap()

    return Triple(credentialRepresentation, scheme, attributes)
}

private fun ConstantIndex.CredentialScheme.matchAgainstIdentifier(
    representation: CredentialRepresentation,
    identifiers: Collection<String>
) = when (representation) {
    PLAIN_JWT -> throw Throwable("PLAIN_JWT not implemented")
    SD_JWT -> sdJwtType in identifiers
    ISO_MDOC -> isoDocType in identifiers
}

private fun InputDescriptor.vctConstraint() =
    constraints?.fields?.firstOrNull { it.path.toString().contains("vct") }

private fun ConstraintFilter.referenceValues() =
    (pattern ?: const?.content)?.let { listOf(it) } ?: enum

/**
 * assumes json claim path pointers don't contain `null`, otherwise only the prefix is shown
 */
@Throws(Throwable::class)
fun DCQLCredentialQuery.extractConsentData(): Triple<CredentialRepresentation, ConstantIndex.CredentialScheme, Collection<SingleClaimReference?>?> {
    val representation = when (format) {
        CredentialFormatEnum.DC_SD_JWT -> SD_JWT
        CredentialFormatEnum.MSO_MDOC -> ISO_MDOC
        else -> PLAIN_JWT
    }

    val scheme = when (this) {
        is DCQLIsoMdocCredentialQuery -> meta.doctypeValue
            .let { AttributeIndex.resolveIsoDoctype(it) }

        is DCQLSdJwtCredentialQuery -> meta.vctValues
            .firstNotNullOfOrNull { AttributeIndex.resolveSdJwtAttributeType(it) }

        is DCQLJwtVcCredentialQuery -> meta.typeValues.list.flatten()
            .filterNot { it == VERIFIABLE_CREDENTIAL }
            .firstNotNullOfOrNull { AttributeIndex.resolveAttributeType(it) }
    } ?: when (this) {
        is DCQLIsoMdocCredentialQuery -> IsoMdocFallbackCredentialScheme(isoDocType = meta.doctypeValue)
        is DCQLSdJwtCredentialQuery -> meta.vctValues
            .firstNotNullOfOrNull { SdJwtFallbackCredentialScheme(sdJwtType = it) }

        is DCQLJwtVcCredentialQuery -> meta.typeValues.list.flatten()
            .filterNot { it == VERIFIABLE_CREDENTIAL }
            .firstNotNullOfOrNull { VcFallbackCredentialScheme(vcType = it) }
    } ?: throw Throwable("No matching scheme for $meta")

    // assuming all claims path pointers are single claim references
    val singleReferenceClaimsQueries = this.claims?.associateWith {
        when (it) {
            is DCQLJsonClaimsQuery -> JsonClaimReference(
                NormalizedJsonPath(it.path.map {
                    when (it) {
                        is DCQLClaimsPathPointerSegment.IndexSegment -> IndexSegment(it.index)
                        is DCQLClaimsPathPointerSegment.NameSegment -> NameSegment(it.name)
                        DCQLClaimsPathPointerSegment.NullSegment -> null
                    }
                }.takeWhile {
                    it != null
                }.filterNotNull())
            )

            is DCQLIsoMdocClaimsQuery -> MdocClaimReference(namespace = it.namespace, claimName = it.claimName)

            is DCQLAmbiguousClaimsQuery -> throw IllegalStateException("Unsupported claims query format: $it")
        }
    }
    return Triple(representation, scheme, singleReferenceClaimsQueries?.values)
}

fun ConstantIndex.CredentialScheme.toJsonElement(
    representation: CredentialRepresentation,
    requestedElements: Collection<String>? = null
): JsonElement {
    val dataElements = this.claimNames
    // TODO move this to credentials libraries
    val complexElements = when (this) {
        EuPidSdJwtScheme -> buildJsonObject {
            put(EuPidSdJwtScheme.SdJwtAttributes.PREFIX_ADDRESS, buildJsonObject {
                with(EuPidSdJwtScheme.SdJwtAttributes.Address) {
                    put(FORMATTED, JsonPrimitive(""))
                    put(COUNTRY, JsonPrimitive(""))
                    put(REGION, JsonPrimitive(""))
                    put(LOCALITY, JsonPrimitive(""))
                    put(POSTAL_CODE, JsonPrimitive(""))
                    put(STREET, JsonPrimitive(""))
                    put(HOUSE_NUMBER, JsonPrimitive(""))
                }
            })
            put(EuPidSdJwtScheme.SdJwtAttributes.PREFIX_PLACE_OF_BIRTH, buildJsonObject {
                with(EuPidSdJwtScheme.SdJwtAttributes.PlaceOfBirth) {
                    put(COUNTRY, JsonPrimitive(""))
                    put(REGION, JsonPrimitive(""))
                    put(LOCALITY, JsonPrimitive(""))
                }
            })
            put(EuPidSdJwtScheme.SdJwtAttributes.NATIONALITIES, buildJsonArray { })
        }

        is EhicScheme -> buildJsonObject {
            put(EhicScheme.Attributes.PREFIX_ISSUING_AUTHORITY, buildJsonObject {
                with(EhicScheme.Attributes.IssuingAuthority) {
                    put(ID, JsonPrimitive(""))
                    put(NAME, JsonPrimitive(""))
                }
            })
            put(EhicScheme.Attributes.PREFIX_AUTHENTIC_SOURCE, buildJsonObject {
                with(EhicScheme.Attributes.AuthenticSource) {
                    put(ID, JsonPrimitive(""))
                    put(NAME, JsonPrimitive(""))
                }
            })
        }

        is CertificateOfResidenceScheme -> buildJsonObject {
            put(CertificateOfResidenceDataElements.RESIDENCE_ADDRESS, buildJsonObject {
                CertificateOfResidenceDataElements.Address.ALL_ELEMENTS.forEach {
                    put(it, JsonPrimitive(""))
                }
            })
        }

        is CompanyRegistrationScheme -> buildJsonObject {
            with(CompanyRegistrationDataElements) {
                put(REGISTERED_ADDRESS, buildJsonObject {
                    CompanyRegistrationDataElements.Address.ALL_ELEMENTS.forEach {
                        put(it, JsonPrimitive(""))
                    }
                })
                put(POSTAL_ADDRESS, buildJsonObject {
                    CompanyRegistrationDataElements.Address.ALL_ELEMENTS.forEach {
                        put(it, JsonPrimitive(""))
                    }
                })
                put(COMPANY_ACTIVITY, buildJsonObject {
                    CompanyRegistrationDataElements.CompanyActivity.ALL_ELEMENTS.forEach {
                        put(it, JsonPrimitive(""))
                    }
                })
                put(COMPANY_CONTACT_DATA, buildJsonObject {
                    CompanyRegistrationDataElements.ContactData.ALL_ELEMENTS.forEach {
                        put(it, JsonPrimitive(""))
                    }
                })
                put(BRANCH, buildJsonObject {
                    with(CompanyRegistrationDataElements.Branch) {
                        put(NAME, JsonPrimitive(""))
                        put(EUID, JsonPrimitive(""))
                        put(ACTIVITY, buildJsonObject {
                            CompanyRegistrationDataElements.CompanyActivity.ALL_ELEMENTS.forEach {
                                put(it, JsonPrimitive(""))
                            }
                        })
                        put(POSTAL_ADDRESS, buildJsonObject {
                            CompanyRegistrationDataElements.Address.ALL_ELEMENTS.forEach {
                                put(it, JsonPrimitive(""))
                            }
                        })
                        put(REGISTERED_ADDRESS, buildJsonObject {
                            CompanyRegistrationDataElements.Address.ALL_ELEMENTS.forEach {
                                put(it, JsonPrimitive(""))
                            }
                        })
                    }
                })
            }
        }

        else -> buildJsonObject {
        }
    }

    return (dataElements + (requestedElements ?: listOf())).associateWith { "" }.let { attributes ->
        when (representation) {
            PLAIN_JWT -> vckJsonSerializer.encodeToJsonElement(attributes + ("type" to this.vcType))
            SD_JWT -> buildJsonObject {
                addSdJwtDummyMetadata()
                attributes.forEach {
                    put(it.key, JsonPrimitive(it.value))
                }
                put("vct", sdJwtType)
                complexElements.forEach {
                    put(it.key, it.value)
                }
            }

            ISO_MDOC -> vckJsonSerializer.encodeToJsonElement(mapOf(this.isoNamespace to attributes))
        }
    }
}

private fun JsonObjectBuilder.addSdJwtDummyMetadata() {
    put("iss", "")
    put("sub", "")
    put("nbf", 0)
    put("iat", 0)
    put("exp", 0)
    put("cnf", buildJsonObject { })
    put("status", buildJsonObject { })
}

fun Throwable.enrichMessage() = when (this) {
    is ConstraintFieldsEvaluationException -> "$message ${constraintFieldExceptions.keys}"
    else -> message ?: toString()
}

// TODO Replace with function from JSONPath
private fun ConstraintField.toNormalizedJsonPath(): NormalizedJsonPath? =
    path.firstOrNull()?.removePrefix("$")?.run {
        NormalizedJsonPath(
            if (contains("[")) {
                segmentsByAngle()
            } else if (contains(".")) {
                segmentsByDot()
            } else {
                fallback()
            }
        )
    }

private fun String.segmentsByAngle() = split("[")
    .filter { it.isNotEmpty() }
    .map { NameSegment(it.removeSuffix("]").unquote()) }

private fun String.segmentsByDot() = split(".")
    .filter { it.isNotEmpty() }
    .map { NameSegment(it) }

private fun String.unquote() = removePrefix("'").removePrefix("\"")
    .removeSuffix("\"").removeSuffix("'")

private fun String.fallback(): List<NameSegment> = listOf(NameSegment(this))

fun NormalizedJsonPath.memberName(id: Int) = this.segments.map { (it as NameSegment).memberName }.getOrNull(id)

fun NormalizedJsonPath.minus(name: String) =
    NormalizedJsonPath(this.segments.filter {
        (it as NameSegment).memberName != name
    }
    )

fun Array<DocRequest>.toDifInputDescriptorList() = this.map {
    val itemsRequest = it.itemsRequest.value
    DifInputDescriptor(
        id = itemsRequest.docType,
        format = FormatHolder(msoMdoc = FormatContainerJwt()),
        constraints = Constraint(fields = itemsRequest.namespaces.flatMap { requestedNamespace ->
            requestedNamespace.value.entries.map { requestedAttribute ->
                ConstraintField(
                    path = listOf(
                        NormalizedJsonPath(
                            NameSegment(requestedNamespace.key),
                            NameSegment(requestedAttribute.dataElementIdentifier),
                        ).toString()
                    ), intentToRetain = requestedAttribute.intentToRetain
                )
            }
        }.toSet())
    )
}

@Composable
fun Triple<CredentialRepresentation, ConstantIndex.CredentialScheme, Collection<SingleClaimReference?>?>.toCredentialQueryUiModel(): DCQLCredentialQueryUiModel {
    val (representation, scheme, attributePaths) = this
    return DCQLCredentialQueryUiModel(
        credentialRepresentationLocalized = representation.uiLabel(),
        credentialSchemeLocalized = scheme.uiLabel(),
        requestedAttributesLocalized = attributePaths?.let { claimReferences ->
            DCQLCredentialQueryUiModelAttributeLabels(
                otherAttributes = claimReferences.count {
                    it == null
                },
                attributesLocalized = claimReferences.filterNotNull().map { path ->
                    catchingUnwrapped {
                        (scheme.getLocalization(path) ?: representation.getMetadataLocalization(path))
                            ?.let { stringResource(it) }
                            ?: path.toString()
                    }.getOrElse { path.toString() }
                }
            )
        },
    )
}

fun ConstantIndex.CredentialRepresentation.getMetadataLocalization(
    claimReference: SingleClaimReference
) = when (claimReference) {
    is JsonClaimReference -> claimReference.normalizedJsonPath.segments.filterIsInstance<NameSegment>()
        .firstOrNull()?.let {
            JwtClaimDefinition.valueOfClaimNameOrNull(it.memberName)?.let { claimDefinition ->
                JwtClaimDefinitionTranslator().translate(claimDefinition)
            }
        }

    is MdocClaimReference -> null
}