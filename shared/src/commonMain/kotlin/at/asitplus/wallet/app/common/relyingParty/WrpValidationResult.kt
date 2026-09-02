package at.asitplus.wallet.app.common.relyingParty

import at.asitplus.etsi.relyingParty.WrpLangString
import at.asitplus.wallet.lib.agent.validation.relyingParty.accessCertificate.WrpacValidationResult
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.RequestDataValidationResult
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.WrprcValidationResult
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.WrprcVerifierInfoValidationResult
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.getPayload
import kotlinx.serialization.Serializable


@Serializable
data class WrpValidationResult(
    val displayInfo: WrpDisplayInfo?,
    val validAttributes: Boolean,
    val validCredentialType: Boolean,
    val wrprcValid: Boolean,
    val wrpacValid: Boolean,
    val requestDataValidationResult: RequestDataValidationResult
) {
    constructor(
        wrprcValidationResult: WrprcValidationResult,
        wrpacValidationResult: WrpacValidationResult
    ) : this(
        displayInfo = wrprcValidationResult.verifierInfoValidationResult.getDisplayInfo(),
        validAttributes =
            wrprcValidationResult.requestDataValidationResult.all { it.value?.credentialAttributesValidity?.all { it.second } == true },
        validCredentialType = wrprcValidationResult.requestDataValidationResult.all { it.value?.credentialTypeValidity == true },
        wrprcValid =
            wrprcValidationResult.verifierInfoValidationResult.isNotEmpty() && wrprcValidationResult.verifierInfoValidationResult.all { it.value?.isValid() == true },
        wrpacValid = wrpacValidationResult.hashValid && wrpacValidationResult.chainValid,
        requestDataValidationResult = wrprcValidationResult.requestDataValidationResult
    )
}

fun WrprcVerifierInfoValidationResult.getDisplayInfo() = this.mapNotNull { (verifierInfo, validationResult) ->
    if (validationResult?.isValid() == true) {
        verifierInfo.getPayload()
    } else {
        null
    }
}.firstOrNull()?.let { validatedPayload ->
    WrpDisplayInfo(
        name = validatedPayload.name,
        purpose = validatedPayload.purpose,
        country = validatedPayload.country,
        infoUri = validatedPayload.infoUri,
        supportUri = validatedPayload.supportUri
    )
}

@Serializable
data class WrpDisplayInfo(
    val name: String?,
    val purpose: List<WrpLangString>?,
    val country: String?,
    val infoUri: String?,
    val supportUri: String?,
)
