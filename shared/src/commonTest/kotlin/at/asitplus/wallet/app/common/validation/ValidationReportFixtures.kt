package at.asitplus.wallet.app.common.validation

import at.asitplus.wallet.lib.data.rfc.tokenStatusList.StatusListInfo
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.primitives.TokenStatus
import at.asitplus.wallet.lib.data.rfc3986.UniformResourceIdentifier
import at.asitplus.wallet.lib.validation.AccessCertificateChecks
import at.asitplus.wallet.lib.validation.CheckOutcome
import at.asitplus.wallet.lib.validation.CheckOutcome.Blocked
import at.asitplus.wallet.lib.validation.CheckOutcome.NotApplicable
import at.asitplus.wallet.lib.validation.CheckOutcome.Passed
import at.asitplus.wallet.lib.validation.CredentialChecks
import at.asitplus.wallet.lib.validation.RegistrationCertificateChecks
import at.asitplus.wallet.lib.validation.RelyingPartyChecks
import at.asitplus.wallet.lib.validation.RequiredClaimsValidation
import at.asitplus.wallet.lib.validation.RequestObjectChecks
import at.asitplus.wallet.lib.validation.StatusListTokenChecks
import at.asitplus.wallet.lib.validation.StatusMechanismValidation
import at.asitplus.wallet.lib.validation.StatusValidation
import at.asitplus.wallet.lib.validation.TimelinessValidation
import at.asitplus.wallet.lib.validation.TrustPolicy
import at.asitplus.wallet.lib.validation.TrustValidation
import at.asitplus.wallet.lib.validation.ValidationPolicy
import at.asitplus.wallet.lib.validation.ValidationReport
import kotlin.time.Instant

/**
 * Reports as VC-K creates them for a relying party at [now], with every check passed unless a parameter says otherwise.
 */
internal open class ValidationReportFixtures(val now: Instant = Instant.parse("2026-10-13T12:00:00Z")) {
    private val policy = ValidationPolicies.strict
    private val noStatus = StatusValidation(claim = NotApplicable, agreement = NotApplicable)

    fun request(
        signature: CheckOutcome = Passed,
        timeliness: CheckOutcome = Passed,
        walletNonce: CheckOutcome = Passed,
        relyingParty: ValidationReport? = null,
    ) = ValidationReport.requestObject(
        checks = RequestObjectChecks(
            parsing = Passed,
            signature = signature,
            // An unsigned request establishes no relying party to trust
            trust = TrustValidation(if (signature == NotApplicable) NotApplicable else Passed, source = WRPAC_PROVIDERS),
            timeliness = timeliness,
            clientId = Passed,
            walletNonce = walletNonce,
            expectedOrigin = NotApplicable,
        ),
        policy = policy,
        verifierAttestation = null,
        relyingParty = relyingParty,
        evaluatedAt = now,
    )

    fun relyingParty(
        accessCertificate: ValidationReport = accessCertificate(),
        registrationCertificate: ValidationReport? = registrationCertificate(),
        registration: CheckOutcome = Passed,
    ) = ValidationReport.relyingParty(
        checks = RelyingPartyChecks(requestData = Passed, registration = registration),
        accessCertificate = accessCertificate,
        registrationCertificates = listOfNotNull(registrationCertificate),
        evaluatedAt = now,
    )

    fun accessCertificate(
        parsing: CheckOutcome = Passed,
        signature: CheckOutcome = Passed,
        trust: CheckOutcome = Passed,
        timeliness: CheckOutcome = Passed,
    ) = ValidationReport.accessCertificate(
        checks = AccessCertificateChecks(
            parsing = parsing,
            signature = signature,
            trust = TrustValidation(trust, source = WRPAC_PROVIDERS),
            timeliness = timeliness,
            clientId = NotApplicable,
        ),
        policy = policy,
        evaluatedAt = now,
    )

    fun registrationCertificate(
        signature: CheckOutcome = Passed,
        status: StatusValidation = noStatus,
        linkage: CheckOutcome = Passed,
        requestAuthorization: CheckOutcome = Passed,
    ) = ValidationReport.registrationCertificate(
        checks = RegistrationCertificateChecks(
            parsing = Passed,
            signature = signature,
            trust = TrustValidation(Passed, source = "WRPRC providers"),
            timeliness = Passed,
            status = status,
            linkage = linkage,
            requestAuthorization = requestAuthorization,
        ),
        policy = policy,
        evaluatedAt = now,
    )

    fun credential(
        timeliness: CheckOutcome = Passed,
        status: StatusValidation = noStatus,
        requiredClaims: RequiredClaimsValidation = RequiredClaimsValidation(NotApplicable),
        policy: ValidationPolicy = this.policy,
    ) = ValidationReport.credential(
        checks = CredentialChecks(
            parsing = Passed,
            issuerSignature = Passed,
            issuerTrust = TrustValidation(Passed, source = "pid-providers.json"),
            semantics = Passed,
            disclosedItems = emptyList(),
            requiredClaims = requiredClaims,
            holderBinding = NotApplicable,
            timeliness = TimelinessValidation(timeliness, details = null),
            status = status,
        ),
        policy = policy,
        evaluatedAt = now,
    )

    fun statusListToken(
        retrieval: CheckOutcome = Passed,
        signerTrust: CheckOutcome = Passed,
    ) = ValidationReport.statusListToken(
        checks = if (retrieval == Passed) StatusListTokenChecks(
            retrieval = Passed,
            parsing = Passed,
            signature = Passed,
            signerTrust = TrustValidation(signerTrust),
            subject = Passed,
            timeliness = Passed,
        ) else StatusListTokenChecks(
            retrieval = retrieval,
            parsing = Blocked(),
            signature = Blocked(),
            signerTrust = TrustValidation(Blocked()),
            subject = Blocked(),
            timeliness = Blocked(),
        ),
        signerTrust = TrustPolicy.RequireAuthorizedSigner,
        evaluatedAt = now,
    )

    fun mechanism(token: ValidationReport, outcome: CheckOutcome, observed: TokenStatus?) =
        StatusMechanismValidation(
            reference = StatusListInfo(0u, UniformResourceIdentifier("https://status.example.com/1")),
            token = token,
            outcome = outcome,
            observed = observed,
        )

    companion object {
        const val WRPAC_PROVIDERS = "WRPAC providers"
    }
}
