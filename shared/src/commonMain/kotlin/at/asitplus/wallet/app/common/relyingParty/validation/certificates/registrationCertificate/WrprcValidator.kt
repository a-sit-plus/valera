package at.asitplus.wallet.app.common.relyingParty.validation.certificates.registrationCertificate

import at.asitplus.catchingUnwrapped
import at.asitplus.data.NonEmptyList
import at.asitplus.openid.OpenIdConstants
import at.asitplus.openid.VerifierInfo
import at.asitplus.signum.indispensable.josef.JwsAlgorithm
import at.asitplus.signum.indispensable.josef.JwsCompact
import at.asitplus.signum.indispensable.josef.JwsCompactTyped
import at.asitplus.signum.indispensable.josef.JwsTyped
import at.asitplus.signum.indispensable.pki.X509Certificate
import at.asitplus.signum.indispensable.pki.leaf
import at.asitplus.wallet.app.common.domain.vck.tokenStatusList.StatusListTokenResolver
import at.asitplus.wallet.app.common.relyingParty.validation.WrpRequestValidationData
import at.asitplus.wallet.app.common.relyingParty.validation.certificates.WrpChainValidator
import at.asitplus.wallet.app.common.relyingParty.validation.certificates.accessCertificate.WrpacIdentifierResult
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.StatusList
import at.asitplus.wallet.lib.data.rfc3986.UniformResourceIdentifier
import at.asitplus.wallet.lib.extensions.toView
import at.asitplus.wallet.lib.jws.VerifyJwsSignature
import at.asitplus.wallet.rp.registration.wrprc.WrpPayloadDto
import io.github.aakira.napier.Napier
import kotlin.time.Clock


/**
 * Class to verify a registration certificates validity.
 **/
class WrprcValidator(
    val chainValidator: WrpChainValidator,
    val requestValidator: WrprcRequestValidator,
    val statusListTokenResolver: StatusListTokenResolver
) {
    fun parse(verifierInfo: VerifierInfo): JwsTyped<JwsCompact, WrpPayloadDto>? = run {
        if (!verifierInfo.format.equals(REGISTRATION_CERT_FORMAT, ignoreCase = true)) {
            Napier.w(
                "skipping $this, expected '$REGISTRATION_CERT_FORMAT' but got '${verifierInfo.format}'.",
                tag = LOG_TAG
            )
            return null
        }
        val jwsTyped = catchingUnwrapped {
            JwsCompactTyped<WrpPayloadDto>(verifierInfo.data)
        }.getOrElse {
            Napier.w("$this ($REGISTRATION_CERT_FORMAT) contains invalid JWS data.", tag = LOG_TAG, throwable = it)
            return null
        }
        jwsTyped
    }

    suspend fun validateRequest(
        validationData: WrpRequestValidationData,
        registrationCertValidation: WrprcValidationResult
    ) =
        validationData.request?.let { presentationRequest ->
            requestValidator.requestCheck(presentationRequest, registrationCertValidation.keys).onFailure {
                Napier.e("$it")
            }.getOrNull()
        }

    suspend fun validateVerifierInfoList(
        verifierInfo: NonEmptyList<VerifierInfo>,
        identifierResult: WrpacIdentifierResult
    ): WrprcValidationResult =
        verifierInfo.associateWith { validateVerifierInfo(verifierInfo = it, identifierResult = identifierResult) }

    private suspend fun validateVerifierInfo(
        verifierInfo: VerifierInfo,
        identifierResult: WrpacIdentifierResult
    ): VerifierInfoValidationResult? = parse(verifierInfo)?.let { jwsTyped ->
        val certificateChain = jwsTyped.jws.jwsHeader.certificateChain
        val headerValid = validateHeader(jwsTyped)
        val chainValid = chainValidator.validateChain(
            certificateChain,
            source = "WRPRC $verifierInfo x5c"
        )

        val leafCertificate = certificateChain?.leaf ?: run {
            Napier.w("No leaf certificate", tag = LOG_TAG)
            return null
        }

        val signatureValid = validateSignature(jwsTyped, leafCertificate)
        val payloadValid = validatePayload(jwsTyped)

        val linkageValid =
            validateWrpIdentifierLinkage(identifierResult = identifierResult, jwsTyped = jwsTyped)

        val statusValid = validateWrpStatusList(jwsTyped)

        VerifierInfoValidationResult(
            jwsTyped = jwsTyped,
            signatureValid = signatureValid,
            chainValid = chainValid,
            linkageValid = linkageValid,
            headerValid = headerValid,
            payloadValid = payloadValid,
            statusValid = statusValid,
        )
    }

    private fun validateHeader(jwsTyped: JwsCompactTyped<WrpPayloadDto>): Boolean {
        if (jwsTyped.jws.jwsHeader.type != "rc-wrp+jwt") {
            Napier.e(
                "$jwsTyped has invalid typ in JWS header. " +
                        "expected='rc-wrp+jwt', actual='${jwsTyped.jws.jwsHeader.type}'",
                tag = LOG_TAG
            )
            return false
        }
        if (jwsTyped.jws.jwsHeader.algorithm.identifier != "ES256") {
            Napier.e(
                "$jwsTyped has invalid alg in JWS header. " +
                        "expected='ES256', actual='${jwsTyped.jws.jwsHeader.algorithm.identifier}'",
                tag = LOG_TAG
            )
            return false
        }
        Napier.d("header checks passed for $jwsTyped.", tag = LOG_TAG)
        return true
    }

    private suspend fun validateSignature(
        jwsTyped: JwsCompactTyped<WrpPayloadDto>,
        leafCertificate: X509Certificate
    ): Boolean = catchingUnwrapped {
        val jwsAlgorithm = jwsTyped.jws.jwsHeader.algorithm
        if (jwsAlgorithm !is JwsAlgorithm.Signature) {
            Napier.e("$jwsTyped uses unsupported JWS algorithm.", tag = LOG_TAG)
            return@catchingUnwrapped false
        }
        VerifyJwsSignature().invoke(jwsTyped.jws, leafCertificate.decodedPublicKey.getOrThrow()).getOrThrow().also {
            Napier.d("signature validation passed for $jwsTyped.", tag = LOG_TAG)
        }
        true
    }.getOrDefault(false)

    private fun validatePayload(jwsTyped: JwsCompactTyped<WrpPayloadDto>): Boolean {
        val now = Clock.System.now().epochSeconds
        val payload = jwsTyped.payload
        if (payload.name == null) {
            Napier.e("$jwsTyped is missing required payload claim 'name'.", tag = LOG_TAG)
            return false
        }
        if (payload.intendedUseId == null) {
            Napier.e("$jwsTyped is missing required payload claim 'intendedUseId'.", tag = LOG_TAG)
            return false
        }
        if (payload.srvDescription.isEmpty()) {
            Napier.e("$jwsTyped is missing required payload claim 'srv_description'", tag = LOG_TAG)
            return false
        }
        if (payload.credentials.isEmpty()) {
            Napier.e("$jwsTyped is missing required payload claim 'credentials'.", tag = LOG_TAG)
            return false
        }
        if (payload.exp <= payload.iat) {
            Napier.e(
                "$jwsTyped has invalid temporal claims: exp=${payload.exp} <= iat=${payload.iat}.",
                tag = LOG_TAG
            )
            return false
        }
        if (payload.exp < now) {
            Napier.e(
                "$jwsTyped already expired: exp=${payload.exp} <= now=${now}.",
                tag = LOG_TAG
            )
            return false
        }
        Napier.d("payload checks passed for $jwsTyped.", tag = LOG_TAG)
        return true
    }

    private suspend fun validateWrpStatusList(
        jwsTyped: JwsCompactTyped<WrpPayloadDto>
    ) = catchingUnwrapped {
        jwsTyped.payload.status.statusList.let { statusListDto ->
            val uri = statusListDto.uri
            val idx = statusListDto.idx
            val tokenStatus =
                statusListTokenResolver(uniformResourceIdentifier = UniformResourceIdentifier(uri)).parsedPayload.getOrNull()
                    ?.let {
                        val statusList = (it.revocationList as? StatusList)
                        statusList?.toView()?.getOrNull(idx.toLong())
                    }
            tokenStatus?.isValid ?: false
        }
    }.getOrElse {
        Napier.w("Unable to get status list entry for $jwsTyped")
        false
    }

    /**
     * Validates the linkage between access certificate and registration certificate.
     * Reference: ETSI TS 119 475 V1.2.1 (S18-S20)
     **/
    private fun validateWrpIdentifierLinkage(
        identifierResult: WrpacIdentifierResult,
        jwsTyped: JwsCompactTyped<WrpPayloadDto>
    ): Boolean =
        when (jwsTyped.payload.declaredPersonType()) {
            WrpPersonType.LEGAL -> identifierResult.legalIdentifier
            WrpPersonType.NATURAL -> identifierResult.naturalIdentifier
            null -> return false
        }.let { identifier ->
            return identifier == jwsTyped.payload.sub
        }


    /**
     * Identifies person type based on the populated fields.
     * Reference: ETSI TS 119 475 V1.2.1 (S18-S20)
     **/
    private fun WrpPayloadDto.declaredPersonType(): WrpPersonType? = when {
        subLn != null -> WrpPersonType.LEGAL
        subGn != null || subFn != null -> WrpPersonType.NATURAL
        else -> {
            Napier.w("Cannot detect person type of registration certificate")
            null
        }
    }

    private companion object {
        val LOG_TAG = "[WRPRC] WrprcVerifierInfoValidator"
        val REGISTRATION_CERT_FORMAT = OpenIdConstants.VerifierInfo.REGISTRATION_CERT_FORMAT
    }
}
