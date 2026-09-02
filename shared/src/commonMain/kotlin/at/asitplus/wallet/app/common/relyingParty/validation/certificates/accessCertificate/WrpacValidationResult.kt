package at.asitplus.wallet.app.common.relyingParty.validation.certificates.accessCertificate

import at.asitplus.signum.indispensable.pki.X509Certificate

data class WrpacValidationResult(
    val chain: List<X509Certificate>,
    val chainValid: Boolean,
    val hashValid: Boolean,
    val identifierResult: WrpacIdentifierResult
)