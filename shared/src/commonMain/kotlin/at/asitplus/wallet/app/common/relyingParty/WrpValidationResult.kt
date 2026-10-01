package at.asitplus.wallet.app.common.relyingParty

import at.asitplus.KmmResult
import at.asitplus.etsi.relyingParty.WrpLangString
import at.asitplus.wallet.lib.agent.validation.relyingParty.WrpRegistrationCertificate
import at.asitplus.wallet.lib.agent.validation.relyingParty.accessCertificate.WrpacValidationResult
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.RequestDataValidity
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.WrpCredentialRequest
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.WrprcValidationResult
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.isValid
import kotlinx.serialization.Serializable

/**
 * Validation of the access certificate (WRPAC) and the registration certificate (WRPRC) of a relying party.
 * When a certificate could not be validated at all, e.g. because it could not be parsed, its validation is `null`
 * and its error says why.
 */
@Serializable
data class WrpValidationResult(
    val accessCertificate: WrpacValidation?,
    val accessCertificateError: String? = null,
    val registrationCertificate: WrprcValidation?,
    val registrationCertificateError: String? = null,
) {
    constructor(
        accessCertificate: KmmResult<WrpacValidationResult>,
        registrationCertificate: KmmResult<WrprcValidationResult>,
    ) : this(
        accessCertificate = accessCertificate.getOrNull()?.toWrpacValidation(),
        accessCertificateError = accessCertificate.exceptionOrNull()?.displayText(),
        registrationCertificate = registrationCertificate.getOrNull()?.toWrprcValidation(),
        registrationCertificateError = registrationCertificate.exceptionOrNull()?.displayText(),
    )

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
    /** Whether each registration certificate is valid, `false` also if it could not be validated at all. */
    val validCertificates: List<Boolean>,
    /** Why registration certificates could not be validated at all. */
    val certificateErrors: List<String>,
    val requestDataValidationResults: List<WrpRequestDataValidation>,
)

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
    validCertificates = certificateValidationResults.values.map { it.getOrNull()?.isValid() == true },
    certificateErrors = certificateValidationResults.values.mapNotNull { it.exceptionOrNull()?.displayText() },
    requestDataValidationResults = requestDataValidationResults.map { (request, result) ->
        WrpRequestDataValidation(
            request = request,
            validity = result.getOrNull(),
            error = result.exceptionOrNull()?.displayText(),
        )
    },
)

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
