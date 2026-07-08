package at.asitplus.wallet.app.common.relyingParty.validation.certificates.registrationCertificate

import at.asitplus.catchingUnwrapped
import at.asitplus.openid.VerifierInfo
import at.asitplus.signum.indispensable.josef.JwsCompactTyped
import at.asitplus.wallet.app.common.relyingParty.data.WrpCredentialMeta
import at.asitplus.wallet.app.common.relyingParty.getMetadata
import at.asitplus.wallet.app.common.relyingParty.validation.certificates.registrationCertificate.WrpCredentialRequest.*
import at.asitplus.wallet.rp.registration.wrprc.WrpPayloadDto

fun VerifierInfo.getPayload() = catchingUnwrapped { JwsCompactTyped<WrpPayloadDto>(this.data).payload }.getOrNull()

fun WrpCredentialMeta.WrpDocType.contains(other: WrpCredentialMeta.WrpDocType): Boolean =
    this.doctypeValue == other.doctypeValue

fun WrpCredentialMeta.WrpVctType.contains(other: WrpCredentialMeta.WrpVctType): Boolean =
    this.vctValues.any { other.vctValues.contains(it) }

fun WrpCredentialRequest.getMetadata(): WrpCredentialMeta = when (this) {
    is WrpDcqlCredentialQuery -> this.query.getMetadata()
    is WrpInputDescriptor -> this.query.getMetadata()
    is WrpDocRequest -> TODO()
}

enum class WrpPersonType { LEGAL, NATURAL }