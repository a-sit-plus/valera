package at.asitplus.wallet.app.common.relyingParty.validation.certificates.accessCertificate

import at.asitplus.signum.indispensable.asn1.Asn1Primitive
import at.asitplus.signum.indispensable.pki.X509Certificate
import at.asitplus.wallet.app.common.relyingParty.validation.certificates.accessCertificate.WrpacValidator.Constants.OID_ORGANIZATION_IDENTIFIER
import at.asitplus.wallet.app.common.relyingParty.validation.certificates.accessCertificate.WrpacValidator.Constants.OID_SERIAL_NUMBER


data class WrpacIdentifierResult(
    val legalIdentifier: String? = null,
    val naturalIdentifier: String? = null,
)

fun X509Certificate.getWrpIdentifier() = WrpacIdentifierResult(
    legalIdentifier = getWrpLegalIdentifier(),
    naturalIdentifier = getWrpNaturalIdentifier()
)

fun X509Certificate.getWrpLegalIdentifier() =
    this.tbsCertificate.subjectName.firstOrNull { it.attrsAndValues.any { it.oid == OID_ORGANIZATION_IDENTIFIER } }?.attrsAndValues?.first()?.value.let {
        (it as? Asn1Primitive)?.content?.decodeToString()
    }

fun X509Certificate.getWrpNaturalIdentifier() =
    this.tbsCertificate.subjectName.firstOrNull { it.attrsAndValues.any { it.oid == OID_SERIAL_NUMBER } }?.attrsAndValues?.first()?.value.let {
        (it as? Asn1Primitive)?.content?.decodeToString()
    }