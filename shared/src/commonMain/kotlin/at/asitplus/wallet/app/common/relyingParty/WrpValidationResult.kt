package at.asitplus.wallet.app.common.relyingParty

import at.asitplus.KmmResult
import at.asitplus.etsi.relyingParty.WrpLangString
import at.asitplus.wallet.lib.agent.validation.relyingParty.WrpRegistrationCertificate
import at.asitplus.wallet.lib.agent.validation.relyingParty.accessCertificate.WrpacValidationResult
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.RequestDataValidity
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.WrpCredentialRequest
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.WrprcValidationResult
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.isValid
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.primitives.TokenStatus
import kotlinx.serialization.Serializable

/**
 * Validation of the access certificate (WRPAC) and the registration certificate (WRPRC) of a relying party.
 * When a certificate could not be validated at all, e.g. because it could not be parsed, its validation is `null`
 * and its error says why. When the request does not contain a certificate, its validation and its error are `null`.
 */
@Serializable
data class WrpValidationResult(
    val accessCertificate: WrpacValidation?,
    val accessCertificateError: String? = null,
    val registrationCertificate: WrprcValidation?,
    val registrationCertificateError: String? = null,
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

    val requestDataValidationResults: List<WrpRequestDataValidation>
        get() = registrationCertificate?.requestDataValidationResults.orEmpty()
}

@Serializable
data class WrpacValidation(
    val validLinkage: Boolean,
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

fun WrpacValidationResult.toWrpacValidation() = WrpacValidation(validLinkage = validLinkage)

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

/** Messages of this exception and its causes, e.g. the parsing error that caused a validation to fail. */
internal fun Throwable.displayText(): String = generateSequence(this) { it.cause }
    .mapNotNull { it.message }
    .distinct()
    .joinToString(": ")
    .ifEmpty { this::class.simpleName ?: "Unknown error" }

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
