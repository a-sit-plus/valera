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
import at.asitplus.wallet.ehic.EhicScheme
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.eupidsdjwt.EuPidSdJwtScheme
import at.asitplus.wallet.healthid.HealthIdScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.agent.validation.CredentialFreshnessSummary
import at.asitplus.wallet.lib.data.AttributeIndex
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation.ISO_MDOC
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation.PLAIN_JWT
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation.SD_JWT
import at.asitplus.wallet.lib.data.dif.PresentationExchangeInputEvaluator
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.primitives.TokenStatusValidationResult
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.oidvci.toFormat
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import at.asitplus.wallet.por.PowerOfRepresentationDataElements
import at.asitplus.wallet.por.PowerOfRepresentationScheme
import at.asitplus.wallet.taxid.TaxIdScheme
import at.asitplus.wallet.taxid.TaxId2025Scheme
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import ui.composables.CredentialStatusState

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
    } ?: throw Throwable("Missing scheme for $credentialIdentifiers")

    val matchedCredentialIdentifier = when (credentialRepresentation) {
        PLAIN_JWT -> throw Throwable("PLAIN_JWT not implemented")
        SD_JWT -> if (scheme.sdJwtType in credentialIdentifiers) scheme.sdJwtType else scheme.isoNamespace
        ISO_MDOC -> scheme.isoDocType
    }

    val constraintsMap =
        PresentationExchangeInputEvaluator.evaluateInputDescriptorAgainstCredential(
            inputDescriptor = this,
            credentialClaimStructure = scheme.toJsonElement(credentialRepresentation),
            credentialFormat = credentialRepresentation.toFormat(),
            credentialScheme = matchedCredentialIdentifier,
            fallbackFormatHolder = this.format,
            pathAuthorizationValidator = { true },
        ).getOrThrow()

    val attributes = constraintsMap.mapNotNull {
        val path = it.value.map { it.normalizedJsonPath }.firstOrNull() ?: return@mapNotNull null
        val optional = it.key.optional != false // optional by default
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

private fun InputDescriptor.vctConstraint() =
    constraints?.fields?.firstOrNull { it.path.toString().contains("vct") }

private fun ConstraintFilter.referenceValues() =
    (pattern ?: const?.content)?.let { listOf(it) } ?: enum

fun DCQLCredentialQuery.extractConsentData(): Triple<CredentialRepresentation, ConstantIndex.CredentialScheme, List<NormalizedJsonPath>> {
    val representation = when (format) {
        CredentialFormatEnum.VC_SD_JWT,
        CredentialFormatEnum.DC_SD_JWT -> SD_JWT

        CredentialFormatEnum.MSO_MDOC -> ISO_MDOC
        else -> PLAIN_JWT
    }

    val scheme = when (this) {
        is DCQLIsoMdocCredentialQuery -> meta?.doctypeValue
            ?.let { AttributeIndex.resolveIsoDoctype(it) }

        is DCQLSdJwtCredentialQuery -> meta?.vctValues
            ?.firstNotNullOfOrNull { AttributeIndex.resolveSdJwtAttributeType(it) }

        is DCQLCredentialQueryInstance -> null
    } ?: throw Throwable("No matching scheme for $meta")

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
        EuPidScheme -> this.claimNames + EuPidScheme.Attributes.PORTRAIT_CAPTURE_DATE
        ConstantIndex.AtomicAttribute2023, IdAustriaScheme, EuPidSdJwtScheme, MobileDrivingLicenceScheme, HealthIdScheme, EhicScheme, TaxIdScheme, TaxId2025Scheme -> this.claimNames
        // TODO Use: this.claim names for all schemes
        PowerOfRepresentationScheme -> PowerOfRepresentationDataElements.ALL_ELEMENTS
        CertificateOfResidenceScheme -> CertificateOfResidenceDataElements.ALL_ELEMENTS
        CompanyRegistrationScheme -> CompanyRegistrationDataElements.ALL_ELEMENTS
        else -> TODO("${this::class.simpleName} not implemented in jsonElementBuilder yet")
    }

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
            put(EuPidSdJwtScheme.SdJwtAttributes.PREFIX_AGE_EQUAL_OR_OVER, buildJsonObject {
                with(EuPidSdJwtScheme.SdJwtAttributes.AgeEqualOrOver) {
                    put(EQUAL_OR_OVER_12, JsonPrimitive(""))
                    put(EQUAL_OR_OVER_13, JsonPrimitive(""))
                    put(EQUAL_OR_OVER_14, JsonPrimitive(""))
                    put(EQUAL_OR_OVER_16, JsonPrimitive(""))
                    put(EQUAL_OR_OVER_18, JsonPrimitive(""))
                    put(EQUAL_OR_OVER_21, JsonPrimitive(""))
                    put(EQUAL_OR_OVER_25, JsonPrimitive(""))
                    put(EQUAL_OR_OVER_60, JsonPrimitive(""))
                    put(EQUAL_OR_OVER_62, JsonPrimitive(""))
                    put(EQUAL_OR_OVER_65, JsonPrimitive(""))
                    put(EQUAL_OR_OVER_68, JsonPrimitive(""))
                }
            })
            put(EuPidSdJwtScheme.SdJwtAttributes.PREFIX_PLACE_OF_BIRTH, buildJsonObject {
                with(EuPidSdJwtScheme.SdJwtAttributes.PlaceOfBirth) {
                    put(COUNTRY, JsonPrimitive(""))
                    put(REGION, JsonPrimitive(""))
                    put(LOCALITY, JsonPrimitive(""))
                }
            })
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

        else -> buildJsonObject { }
    }

    return dataElements.associateWith { "" }.let { attributes ->
        when (representation) {
            PLAIN_JWT -> vckJsonSerializer.encodeToJsonElement(attributes + ("type" to this.vcType))
            SD_JWT -> buildJsonObject {
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

fun CredentialStatusState.Success.isInvalid(): Boolean =
    freshness?.tokenStatusValidationResult is TokenStatusValidationResult.Invalid

fun CredentialFreshnessSummary.isInvalid(): Boolean =
    (tokenStatusValidationResult is TokenStatusValidationResult.Invalid)
