package at.asitplus.wallet.app.common.dcapi

import at.asitplus.dcapi.request.IsoMdocRequest
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer

/**
 * Decodes the ISO mdoc DC API request [rawRequest] (ISO/IEC 18013-7 Annex C) as received from the platform, and keeps
 * it as the `jsonString` of the result. Its reader authentication signs the encoded items requests as received, which
 * re-encoding them may change, e.g. the order of the members of a `zkRequest`, so that the signature no longer
 * verifies, see [withRawIsoMdocRequest].
 */
fun isoMdocDcApiRequest(
    rawRequest: String,
    callingOrigin: String,
    credentialIds: Collection<String>? = null,
    callingPackageName: String? = null,
) = RequestParametersFrom.IsoMdocDcApi(
    parameters = RequestParametersFrom.IsoMdocDcApi.IsoMdocRequestWrapper(
        joseCompliantSerializer.decodeFromString<IsoMdocRequest>(rawRequest)
    ),
    jsonString = rawRequest,
    credentialIds = credentialIds,
    callingPackageName = callingPackageName,
    callingOrigin = callingOrigin,
)

/**
 * Decodes the ISO mdoc request of [this] again from its raw `jsonString`, see [isoMdocDcApiRequest], as decoding a
 * serialized [RequestParametersFrom] yields the re-encoded request.
 */
fun RequestParametersFrom.DcApiRequest.withRawIsoMdocRequest(): RequestParametersFrom.DcApiRequest = when (this) {
    is RequestParametersFrom.IsoMdocDcApi -> copy(
        parameters = RequestParametersFrom.IsoMdocDcApi.IsoMdocRequestWrapper(
            joseCompliantSerializer.decodeFromString<IsoMdocRequest>(jsonString)
        )
    )

    else -> this
}
