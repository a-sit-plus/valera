package at.asitplus.wallet.app.common.relyingParty.validation.certificates.registrationCertificate

import at.asitplus.catchingUnwrapped
import at.asitplus.dif.InputDescriptor
import at.asitplus.iso.DocRequest
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment.NameSegment
import at.asitplus.openid.VerifierInfo
import at.asitplus.openid.dcql.DCQLCredentialQuery
import at.asitplus.wallet.app.common.extractConsentData
import at.asitplus.wallet.app.common.relyingParty.data.WrpCredentialMeta
import at.asitplus.wallet.app.common.relyingParty.getMetadata
import at.asitplus.wallet.app.common.relyingParty.toSingleClaimReference
import at.asitplus.wallet.app.common.relyingParty.validation.certificates.registrationCertificate.WrpCredentialRequest.*
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation.*
import at.asitplus.wallet.lib.data.CredentialPresentationRequest
import at.asitplus.wallet.lib.data.CredentialPresentationRequest.DCQLRequest
import at.asitplus.wallet.lib.data.CredentialPresentationRequest.PresentationExchangeRequest
import at.asitplus.wallet.lib.data.CredentialScheme
import at.asitplus.wallet.rp.registration.wrprc.WrpPayloadDto
import data.credentials.JsonClaimReference
import data.credentials.MdocClaimReference
import data.credentials.SingleClaimReference
import io.github.aakira.napier.Napier
import kotlinx.serialization.Serializable

typealias RequestCredentialAttributesValidity = List<Pair<SingleClaimReference, Boolean>>
typealias RequestDataValidationResult = Map<String, RequestDataValidity?>

@Serializable
data class RequestDataValidity(
    val credentialTypeValidity: Boolean,
    val credentialAttributesValidity: RequestCredentialAttributesValidity,
)

/*
Sealed interface for supported request types to allow generic validation and serialized transport.
 */
@Serializable
sealed interface WrpCredentialRequest {
    val id: String

    @Serializable
    data class WrpDcqlCredentialQuery(val query: DCQLCredentialQuery, override val id: String = query.id.string) :
        WrpCredentialRequest

    @Serializable
    data class WrpDocRequest(val query: DocRequest, override val id: String = TODO()) : WrpCredentialRequest

    @Serializable
    data class WrpInputDescriptor(val query: InputDescriptor, override val id: String = query.id) : WrpCredentialRequest
}

fun CredentialPresentationRequest.toWrpCredentialRequest() = when (this) {
    is DCQLRequest -> this.dcqlQuery.credentials.map {
        WrpDcqlCredentialQuery(it)
    }.toSet()

    is PresentationExchangeRequest -> this.presentationDefinition.inputDescriptors.map {
        WrpInputDescriptor(it)
    }.toSet()

    is CredentialPresentationRequest.IsoDeviceRetrieval -> TODO()
}

/**
 * Class to validate a presentation request against registration certificates.
 **/
class WrprcRequestValidator {
    suspend fun requestCheck(
        presentationRequest: CredentialPresentationRequest,
        verifierInfos: Set<VerifierInfo>,
    ): Result<RequestDataValidationResult> = catchingUnwrapped {
        matchRequestToVerifierInfo(presentationRequest, verifierInfos).mapNotNull { (request, verifierInfo) ->
            validateCredentialRequest(request, verifierInfo)
        }.toMap()
    }

    /**
     * Matches credential request to a corresponding registration certificate.
     * Falls back to a registration certificate which at least matches the credential type.
     */
    private fun matchCredentialToVerifierInfo(
        request: WrpCredentialRequest,
        verifierInfos: Set<VerifierInfo>,
    ) = request to when (request) {
        is WrpDcqlCredentialQuery -> {
            val queryMeta = request.query.getMetadata()
            verifierInfos.firstOrNull { it.credentialIds?.contains(request.query.id.string) == true } ?: run {
                matchVerifierInfoFallback(queryMeta, verifierInfos)
            }
        }

        is WrpInputDescriptor -> {
            val queryMeta = request.query.getMetadata()
            matchVerifierInfoFallback(queryMeta, verifierInfos)
        }

        is WrpDocRequest -> TODO()
    }

    private fun matchVerifierInfoFallback(
        queryMeta: WrpCredentialMeta,
        verifierInfos: Set<VerifierInfo>,
    ) = when (queryMeta) {
        is WrpCredentialMeta.WrpDocType -> {
            verifierInfos.firstOrNull {
                it.getPayload()?.credentials?.any { credential ->
                    catchingUnwrapped {
                        queryMeta.contains(credential.meta.toDomain() as WrpCredentialMeta.WrpDocType)
                    }.getOrElse {
                        Napier.e("matchVerifierInfoFallback: failed to cast ${credential.meta}")
                        false
                    }
                } == true
            }
        }

        is WrpCredentialMeta.WrpVctType -> {
            verifierInfos.firstOrNull {
                it.getPayload()?.credentials?.any { credential ->
                    catchingUnwrapped {
                        queryMeta.contains(credential.meta.toDomain() as WrpCredentialMeta.WrpVctType)
                    }.getOrElse {
                        Napier.e("matchVerifierInfoFallback: failed to cast ${credential.meta}")
                        false
                    }
                } == true
            }
        }
    }


    private suspend fun validateCredentialRequest(
        request: WrpCredentialRequest, verifierInfo: VerifierInfo?
    ): Pair<String, RequestDataValidity?>? = catchingUnwrapped {
        request.id to verifierInfo?.getPayload()?.let { payload ->
            RequestDataValidity(
                credentialTypeValidity = checkCredentialTypesValidity(request, payload),
                credentialAttributesValidity = checkAttributesValidity(request, payload)
            )
        }
    }.getOrElse {
        Napier.w("WrprcRequestValidator.validateCredentialRequest failed with:", tag = LOG_TAG, throwable = it)
        null
    }

    private fun matchRequestToVerifierInfo(
        presentationRequest: CredentialPresentationRequest, verifierInfos: Set<VerifierInfo>
    ): Map<WrpCredentialRequest, VerifierInfo?> = presentationRequest.toWrpCredentialRequest().associate {
        matchCredentialToVerifierInfo(it, verifierInfos)
    }

    private suspend fun checkAttributesValidity(
        credentialRequest: WrpCredentialRequest, wrpPayload: WrpPayloadDto
    ): RequestCredentialAttributesValidity = when (credentialRequest) {
        is WrpDcqlCredentialQuery -> {
            credentialRequest.query.extractConsentData().let { (representation, scheme, attributes) ->
                checkAttributes(wrpPayload, representation, scheme, attributes)
            }
        }

        is WrpInputDescriptor -> {
            credentialRequest.query.extractConsentData().let { (representation, scheme, attributes) ->
                checkAttributes(
                    wrpPayload,
                    representation,
                    scheme,
                    attributes.keys.map { it.toSingleClaimReference(representation) })
            }
        }

        is WrpDocRequest -> TODO()
    }


    private fun checkCredentialTypesValidity(
        credentialRequest: WrpCredentialRequest, wrpPayload: WrpPayloadDto
    ): Boolean = checkCredentialTypes(credentialRequest.getMetadata(), wrpPayload)


    private fun checkCredentialTypes(
        metadata: WrpCredentialMeta, wrpPayload: WrpPayloadDto
    ): Boolean = run {
        val metaList = wrpPayload.credentials.map {
            it.meta
        }
        when (metadata) {
            is WrpCredentialMeta.WrpDocType -> {
                metaList.any { meta ->
                    catchingUnwrapped {
                        metadata.contains(meta.toDomain() as WrpCredentialMeta.WrpDocType)
                    }.getOrDefault(false)
                }
            }

            is WrpCredentialMeta.WrpVctType -> {
                metaList.any { meta ->
                    catchingUnwrapped {
                        metadata.contains(meta.toDomain() as WrpCredentialMeta.WrpVctType)
                    }.getOrDefault(false)
                }
            }
        }
    }

    private fun checkAttributes(
        wrpPayload: WrpPayloadDto,
        representation: ConstantIndex.CredentialRepresentation,
        scheme: CredentialScheme,
        attributes: Collection<SingleClaimReference?>?
    ): RequestCredentialAttributesValidity = catchingUnwrapped {
        when (representation) {
            ISO_MDOC -> {
                val listCredentialDto = wrpPayload.credentials.filter {
                    catchingUnwrapped {
                        it.meta.doctypeValue == scheme.isoDocType
                    }.getOrDefault(false)
                }
                attributes?.mapNotNull { attribute ->
                    val claim = attribute as? MdocClaimReference ?: return@mapNotNull null
                    val claimName = claim.claimName
                    attribute to (listCredentialDto.firstOrNull()?.claim?.any { it.path.contains(claimName) } ?: false)
                } ?: throw Throwable("checkAttributes: no claims match request")
            }

            SD_JWT -> {
                val listCredentialDto = wrpPayload.credentials.filter {
                    catchingUnwrapped {
                        it.meta.vctValues ?: run {
                            Napier.w("Sd-jwt but vctValues null", tag = LOG_TAG)
                            return@catchingUnwrapped false
                        }
                        it.meta.vctValues.contains(scheme.sdJwtType)
                    }.getOrDefault(false)
                }
                attributes?.mapNotNull { attribute ->
                    val claim = attribute as? JsonClaimReference ?: return@mapNotNull null
                    val claimName = (claim.normalizedJsonPath.segments.last() as NameSegment).memberName
                    if (claimName == "vct") return@mapNotNull null
                    attribute to (listCredentialDto.firstOrNull()?.claim?.any { it.path.contains(claimName) } ?: false)
                } ?: throw Throwable("checkAttributes: no claims match request")
            }

            PLAIN_JWT -> {
                TODO("PLAIN_JWT not supported")
            }
        }
    }.getOrElse { throw Throwable("checkAttributes: failed with $it") }

    private companion object Constants {
        const val LOG_TAG = "[WrprcRequestValidator]"
    }
}

