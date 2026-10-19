package at.asitplus.wallet.app.common.iso.verifier

import at.asitplus.iso.DeviceResponse
import at.asitplus.iso.Document
import at.asitplus.iso.SessionTranscript
import at.asitplus.wallet.app.common.validation.ValidationPolicies
import at.asitplus.wallet.lib.validation.CredentialValidator
import at.asitplus.wallet.lib.validation.PresentationContext
import at.asitplus.wallet.lib.validation.PresentationValidationInput
import at.asitplus.wallet.lib.validation.ValidationReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Validates the documents of a `DeviceResponse` this wallet received as an ISO/IEC 18013-5 proximity reader under
 * [ValidationPolicies.strict], with the trust anchors and the status list token resolver of the session's
 * [credentialValidator]: the issuer signature and trust, the digests of the disclosed items, time, status, and the
 * device signature over the [SessionTranscript] of the reader session.
 */
class ProximityResponseValidator(
    private val credentialValidator: CredentialValidator,
) {
    /**
     * Validates every document of [deviceResponse] off the UI dispatcher, as verifying it is expensive. Without a
     * [sessionTranscript], the device signature can not be verified, which the reports show.
     *
     * @throws VerifyResponseException if the holder reports an error instead of documents
     */
    suspend fun validate(
        deviceResponse: DeviceResponse,
        sessionTranscript: SessionTranscript?,
    ): List<ProximityDocumentValidation> = withContext(Dispatchers.Default) {
        if (deviceResponse.status != 0U) throw VerifyResponseException("DeviceResponse status: ${deviceResponse.status}")
        val documents = deviceResponse.documents
            ?: throw VerifyResponseException("DeviceResponse contains no documents")
        // The device signature of an mdoc covers the session transcript, but neither a challenge nor an audience
        val context = PresentationContext(challenge = "", audience = "", sessionTranscript = sessionTranscript)
        documents.map { document ->
            ProximityDocumentValidation(
                document = document,
                report = credentialValidator.validate(
                    input = PresentationValidationInput.MdocDocument(document),
                    context = context,
                    policy = ValidationPolicies.strict,
                ).report,
            )
        }
    }
}

/** A [document] as received, with the [report] of validating it, which may have been rejected. */
data class ProximityDocumentValidation(
    val document: Document,
    val report: ValidationReport,
)
