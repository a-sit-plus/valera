package at.asitplus.wallet.app.common.relyingParty.validation.certificates.registrationCertificate

import at.asitplus.openid.VerifierInfo
import at.asitplus.signum.indispensable.josef.JwsCompact
import at.asitplus.signum.indispensable.josef.JwsTyped
import at.asitplus.wallet.app.common.relyingParty.validation.WrpDisplayInfo
import at.asitplus.wallet.rp.registration.wrprc.WrpPayloadDto

typealias WrprcValidationResult = Map<VerifierInfo, VerifierInfoValidationResult?>

const val UNKNOWN_DISPLAY_INFO = "unknown"

data class VerifierInfoValidationResult(
    val jwsTyped: JwsTyped<JwsCompact, WrpPayloadDto>? = null,
    val signatureValid: Boolean,
    val chainValid: Boolean,
    val linkageValid: Boolean,
    val headerValid: Boolean,
    val payloadValid: Boolean,
    val statusValid: Boolean
) {
    fun isValid() =
        (this.signatureValid && this.chainValid && this.linkageValid && this.headerValid && this.payloadValid && this.payloadValid)
}

fun WrprcValidationResult.getDisplayInfo() = this.mapNotNull { (verifierInfo, validationResult) ->
    if (validationResult?.isValid() == true) {
        verifierInfo.getPayload()
    } else {
        null
    }
}.firstOrNull()?.let { validatedPayload ->
    WrpDisplayInfo(
        name = validatedPayload.name ?: UNKNOWN_DISPLAY_INFO,
        purpose = validatedPayload.purpose,
        country = validatedPayload.country,
        infoUri = validatedPayload.infoUri,
        supportUri = validatedPayload.supportUri ?: UNKNOWN_DISPLAY_INFO
    )
}