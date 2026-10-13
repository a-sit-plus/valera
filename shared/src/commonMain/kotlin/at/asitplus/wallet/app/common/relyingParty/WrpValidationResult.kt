package at.asitplus.wallet.app.common.relyingParty

import at.asitplus.KmmResult
import at.asitplus.etsi.relyingParty.WrpLangString
import at.asitplus.wallet.lib.agent.validation.relyingParty.WrpRegistrationCertificate
import at.asitplus.wallet.lib.agent.validation.relyingParty.accessCertificate.WrpacValidationResult
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.RequestCredentialAttributesValidity
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.RequestDataValidity
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.WrpCredentialRequest
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.WrpRegistrationCertificateValidation
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.WrprcValidationResult
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.isValid
import at.asitplus.signum.indispensable.io.X509CertificateBase64Serializer
import at.asitplus.signum.indispensable.pki.X509Certificate
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.primitives.TokenStatus
import kotlinx.serialization.Serializable

/**
 * Validation of the access certificate (WRPAC) and the registration certificate (WRPRC) of a relying party.
 * When a certificate could not be validated at all, e.g. because it could not be parsed, its validation is `null`
 * and its error says why. When the request does not contain a certificate, its validation and its error are `null`.
 *
 * For a multisigned request, these are `null`, and every authenticated signer is validated on its own, see [signers].
 */
@Serializable
data class WrpValidationResult(
    val accessCertificate: WrpacValidation?,
    val accessCertificateError: String? = null,
    val registrationCertificate: WrprcValidation?,
    val registrationCertificateError: String? = null,
    val multiSigned: Boolean = false,
    /** The authenticated signers of a multisigned request, each with the result for its own identity. */
    val signers: List<SignerWrpValidation> = emptyList(),
    /** Why the signers of a multisigned request could not be validated, e.g. because there are too many. */
    val signersError: String? = null,
    /** Credential queries that no registration certificate of the request applies to, see `credential_ids`. */
    val uncoveredCredentialQueryIds: List<String> = emptyList(),
    /** Client identifier of a signed request, see [SignerWrpValidation.clientId] for multisigned requests. */
    val clientId: String? = null,
    /** Leaf of the access certificate chain the request carries, to show it, whether it is valid or not. */
    @Serializable(with = X509CertificateBase64Serializer::class)
    val accessCertificateLeaf: X509Certificate? = null,
    /** Certificate the registration certificate is signed with, i.e. of its registrar, to show it. */
    @Serializable(with = X509CertificateBase64Serializer::class)
    val registrationCertificateSigner: X509Certificate? = null,
) {
    /** Results are `null` for certificates the request does not contain. */
    constructor(
        accessCertificate: KmmResult<WrpacValidationResult>?,
        registrationCertificate: KmmResult<WrprcValidationResult>?,
    ) : this(
        accessCertificate = accessCertificate?.getOrNull()?.toWrpacValidation(),
        accessCertificateError = accessCertificate?.exceptionOrNull()?.displayText(),
        registrationCertificate = registrationCertificate?.getOrNull()?.toWrprcValidation(),
        registrationCertificateError = registrationCertificate?.exceptionOrNull()?.displayText(),
    )

    val accessCertificateMissing: Boolean
        get() = accessCertificate == null && accessCertificateError == null

    val registrationCertificateMissing: Boolean
        get() = registrationCertificate == null && registrationCertificateError == null

    val displayInfo: WrpDisplayInfo?
        get() = registrationCertificate?.displayInfo

    /**
     * Whether this identity takes part in the EU trust framework, i.e. it has a valid access certificate or carries
     * a registration certificate. Other signers of a multisigned request belong to other trust frameworks.
     */
    val isEudiIdentity: Boolean
        get() = accessCertificate != null || !registrationCertificateMissing

    /** Signers of a multisigned request that take part in the EU trust framework, see [isEudiIdentity]. */
    val eudiSigners: List<SignerWrpValidation>
        get() = signers.filter { it.identity.isEudiIdentity }

    /** The identities that are checked: the single one of a request, or the EU ones of a multisigned request. */
    private val identities: List<WrpValidationResult>
        get() = if (multiSigned) eudiSigners.map { it.identity } else listOf(this)

    /** Whether the request carries nothing to check, in which case nothing is shown. */
    val nothingProvided: Boolean
        get() = if (multiSigned) {
            eudiSigners.isEmpty() && signersError == null
        } else {
            accessCertificateMissing && registrationCertificateMissing
        }

    /**
     * Whether the access certificates of the signers name different relying parties, while a multisigned request
     * comes from a single verifier, see OpenID4VP 1.0, A.3.2.2.
     */
    val differentRelyingParties: Boolean
        get() = identities.mapNotNull { it.accessCertificate?.wrpIdentifier }.distinct().size > 1

    val requestDataValidationResults: List<WrpRequestDataValidation>
        get() = identities.flatMap { it.registrationCertificate?.requestDataValidationResults.orEmpty() }

    /**
     * Attributes of credential query [queryId] that are allowed, i.e. allowed by every registration certificate that
     * applies to the query, or `null` if none applies or one could not check it.
     */
    fun allowedAttributes(queryId: String): RequestCredentialAttributesValidity? {
        val validities = requestDataValidationResults.filter {
            (it.request as? WrpCredentialRequest.WrpDcqlCredentialQuery)?.query?.id?.string == queryId
        }.map { it.validity?.credentialAttributesValidity ?: return null }
        val first = validities.firstOrNull() ?: return null
        return first.map { (claim, _) ->
            claim to validities.all { validity -> validity.any { it.first == claim && it.second } }
        }
    }
}

/** Validation of the identity of one authenticated signer of a multisigned request. */
@Serializable
data class SignerWrpValidation(
    val signatureIndex: Int,
    val clientId: String,
    /** Result for this signer only, see [WrpValidationResult.accessCertificate], without signers of its own. */
    val identity: WrpValidationResult,
)

@Serializable
data class WrpacValidation(
    val validLinkage: Boolean,
    /** Identifier of the relying party in the access certificate, see [WrpacValidationResult.identifierResult]. */
    val wrpIdentifier: String? = null,
)

@Serializable
data class WrprcValidation(
    val displayInfo: WrpDisplayInfo?,
    val certificates: List<WrprcCertificateValidation>,
    val requestDataValidationResults: List<WrpRequestDataValidation>,
)

/** Validation of one registration certificate. */
@Serializable
data class WrprcCertificateValidation(
    val valid: Boolean,
    /** Why the certificate could not be validated at all, then the other properties are not evaluated. */
    val error: String? = null,
    /** Whether the certificate is linked to a valid access certificate of the relying party. */
    val validLinkage: Boolean = true,
    /** Status from the status list, `null` if it could not be obtained, see [tokenStatusError]. */
    val tokenStatus: WrpTokenStatus? = null,
    val tokenStatusError: String? = null,
    /** Other checks the certificate failed, e.g. its signature, as technical details, see [failedChecks]. */
    val failedChecks: List<String> = emptyList(),
)

@Serializable
enum class WrpTokenStatus {
    VALID,
    REVOKED,
    SUSPENDED,

    /** An application-specific status, which is not valid either. */
    OTHER,
}

/** Validity of a credential request against the WRPRC, `null` with an [error] if it could not be validated. */
@Serializable
data class WrpRequestDataValidation(
    val request: WrpCredentialRequest,
    val validity: RequestDataValidity?,
    val error: String? = null,
)

fun WrpacValidationResult.toWrpacValidation() = WrpacValidation(
    validLinkage = validLinkage,
    wrpIdentifier = identifierResult?.identifier,
)

fun WrprcValidationResult.toWrprcValidation() = WrprcValidation(
    displayInfo = certificateValidationResults.keys.getDisplayInfo(),
    certificates = certificateValidationResults.values.map { result ->
        result.fold(
            onSuccess = { validation ->
                WrprcCertificateValidation(
                    valid = validation.isValid(),
                    validLinkage = validation.validLinkage,
                    tokenStatus = validation.tokenStatus.getOrNull()?.toWrpTokenStatus(),
                    tokenStatusError = validation.tokenStatus.exceptionOrNull()?.displayText(),
                    failedChecks = validation.failedChecks(),
                )
            },
            onFailure = { WrprcCertificateValidation(valid = false, error = it.displayText()) },
        )
    },
    requestDataValidationResults = requestDataValidationResults.map { (request, result) ->
        WrpRequestDataValidation(
            request = request,
            validity = result.getOrNull(),
            error = result.exceptionOrNull()?.displayText(),
        )
    },
)

private fun TokenStatus.toWrpTokenStatus() = when (this) {
    TokenStatus.Valid -> WrpTokenStatus.VALID
    TokenStatus.Invalid -> WrpTokenStatus.REVOKED
    TokenStatus.Suspended -> WrpTokenStatus.SUSPENDED
    else -> WrpTokenStatus.OTHER
}

/**
 * The checks [this] validation failed, as technical details, apart from its linkage and its status, which are
 * explained on their own, see [WrprcCertificateValidation].
 */
internal fun WrpRegistrationCertificateValidation.failedChecks(): List<String> = listOfNotNull(
    "Header invalid (typ or alg)".takeUnless { validHeader },
    "Signature invalid".takeUnless { validSignature },
    "Certificate chain not trusted".takeUnless { validChain },
    "Payload invalid, e.g. expired".takeUnless { validPayload },
)

/** Messages of this exception and its causes, e.g. the parsing error that caused a validation to fail. */
internal fun Throwable.displayText(): String = generateSequence(this) { it.cause }
    .mapNotNull { it.message }
    .distinct()
    .joinToString(": ")
    .ifEmpty { this::class.simpleName ?: "Unknown error" }

/** Certificate [this] registration certificate is signed with, if it carries one that can be parsed. */
fun WrpRegistrationCertificate.signerCertificate(): X509Certificate? = when (this) {
    is WrpRegistrationCertificate.WrpJwtRegistrationCertificate -> jwsTyped.jws.jwsHeader.certificateChain?.firstOrNull()
    is WrpRegistrationCertificate.WrpCwtRegistrationCertificate ->
        (cose.protectedHeader.certificateChain ?: cose.unprotectedHeader?.certificateChain)?.firstOrNull()
            ?.let { X509Certificate.decodeFromDerSafe(it).getOrNull() }
}

fun Collection<WrpRegistrationCertificate>.getDisplayInfo() =
    this.map { registrationCertificate ->
        registrationCertificate.payload.let { payload ->
            WrpDisplayInfo(
                name = payload.name,
                purpose = payload.purpose,
                country = payload.country,
                infoUri = payload.infoUri,
                supportUri = payload.supportUri
            )
        }
    }.firstOrNull()

@Serializable
data class WrpDisplayInfo(
    val name: String?,
    val purpose: List<WrpLangString>?,
    val country: String?,
    val infoUri: String?,
    val supportUri: String?,
)
