package at.asitplus.wallet.app.common.relyingParty

import at.asitplus.etsi.relyingParty.WrpLangString
import at.asitplus.wallet.lib.agent.validation.relyingParty.WrpRegistrationCertificate
import at.asitplus.wallet.lib.agent.validation.relyingParty.accessCertificate.WrpacValidationResult
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.RequestDataValidation
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.WrpRegistrationCertificateValidation
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.WrprcValidationResult
import kotlinx.serialization.Serializable


@Serializable
data class WrpValidationResult(
    val displayInfo: WrpDisplayInfo?,
    val validAttributes: Boolean,
    val validCredentialType: Boolean,
    /** Whether the registration certificate is valid, apart from its binding to the relying party, see [validIdentifier]. */
    val validCertificate: Boolean,
    val requestDataValidationResult: RequestDataValidation,
    /** Whether the registration certificate names the relying party identified by the access certificate. */
    val validIdentifier: Boolean = true,
    /** Whether the status of a registration certificate could not be obtained, e.g. from an unreachable status list. */
    val statusListUnresolved: Boolean = false,
) {
    constructor(
        wrprcValidationResult: WrprcValidationResult,
        wrpacValidationResult: WrpacValidationResult
    ) : this(
        displayInfo = wrprcValidationResult.certificateValidation.getDisplayInfo(),
        validAttributes =
            wrprcValidationResult.requestDataValidation.toMap()
                .all { it.value.credentialAttributesValidity.all { it.second } },
        validCredentialType = wrprcValidationResult.requestDataValidation.toMap()
            .all { it.value.credentialTypeValidity },
        validCertificate = wrprcValidationResult.certificateValidation.values.all {
            it != null && it.validHeader && it.validSignature && it.validChain && it.validPayload && it.validStatusList
        },
        requestDataValidationResult = wrprcValidationResult.requestDataValidation,
        validIdentifier = wrprcValidationResult.certificateValidation.values.all { it?.validLinkage == true } &&
                wrpacValidationResult.validLinkage && wrpacValidationResult.identifierResult != null,
        statusListUnresolved = wrprcValidationResult.certificateValidation.values.any { it?.statusListResolved == false },
    )
}

fun Map<WrpRegistrationCertificate, WrpRegistrationCertificateValidation?>.getDisplayInfo() =
    this.map { (registrationCertificate, validation) ->
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
