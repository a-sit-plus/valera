package at.asitplus.wallet.app.common.relyingParty

import at.asitplus.KmmResult
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.info_text_registration_cert_not_evaluated
import at.asitplus.valera.resources.label_access_cert
import at.asitplus.valera.resources.label_registration_cert
import at.asitplus.wallet.app.common.relyingParty.ui.toWrprcRequestValidationData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WrprcRequestValidationDataTest {
    @Test
    fun failedValidationKeepsMessagesOfCauses() {
        val result = WrpValidationResult(
            accessCertificate = KmmResult.failure(IllegalArgumentException("untrusted WRPAC")),
            registrationCertificate = KmmResult.failure(
                IllegalArgumentException("Request must contain exactly one WRPRC", Exception("Invalid JWS"))
            ),
        )

        assertNull(result.accessCertificate)
        assertEquals("untrusted WRPAC", result.accessCertificateError)
        assertNull(result.registrationCertificate)
        assertEquals("Request must contain exactly one WRPRC: Invalid JWS", result.registrationCertificateError)
    }

    @Test
    fun unparseableRegistrationCertificateIsShownWithItsError() {
        val result = WrpValidationResult(
            accessCertificate = WrpacValidation(validLinkage = true),
            registrationCertificate = null,
            registrationCertificateError = "Request must contain exactly one WRPRC: Invalid JWS",
        )

        val data = result.toWrprcRequestValidationData()

        assertEquals(listOf(Res.string.label_access_cert, Res.string.label_registration_cert), data.map { it.text })
        assertEquals(true, data[0].validity)
        assertEquals(false, data[1].validity)
        assertEquals(Res.string.info_text_registration_cert_not_evaluated, data[1].infoInvalid)
        assertEquals(listOf("Request must contain exactly one WRPRC: Invalid JWS"), data[1].errors)
    }

    @Test
    fun invalidAccessCertificateIsShownNextToRegistrationCertificate() {
        val result = WrpValidationResult(
            accessCertificate = null,
            accessCertificateError = "untrusted WRPAC",
            registrationCertificate = WrprcValidation(
                displayInfo = null,
                validCertificates = listOf(false),
                certificateErrors = listOf("no certificate chain"),
                requestDataValidationResults = emptyList(),
            ),
        )

        val data = result.toWrprcRequestValidationData()

        assertEquals(false, data[0].validity)
        assertEquals(listOf("untrusted WRPAC"), data[0].errors)
        assertEquals(false, data[1].validity)
        assertEquals(listOf("no certificate chain"), data[1].errors)
        assertEquals(4, data.size)
    }
}
