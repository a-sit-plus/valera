package at.asitplus.wallet.app.common.validation

import at.asitplus.wallet.lib.validation.RequiredClaimsPolicy
import at.asitplus.wallet.lib.validation.StatusPolicy
import at.asitplus.wallet.lib.validation.TrustPolicy
import at.asitplus.wallet.lib.validation.ValidationPolicy
import kotlin.time.Duration.Companion.minutes

/**
 * The policies the wallet validates with. Reports are always created under [strict], so that they show every problem,
 * and the user decides whether to continue past problems that only [strict] rejects, e.g. an untrusted relying party.
 */
object ValidationPolicies {
    private val timeLeeway = 5.minutes

    /** Requires trusted signers, valid time claims, and an accepted status, wherever the artifact has one. */
    val strict = ValidationPolicy(
        trust = TrustPolicy.RequireAuthorizedSigner,
        status = StatusPolicy.ValidateIfPresent(signerTrust = TrustPolicy.RequireAuthorizedSigner),
        timeLeeway = timeLeeway,
        requireTimeliness = true,
    )

    /**
     * [strict], and additionally requires an issued credential to contain every claim the metadata of its type describe
     * as mandatory, so that the user sees which claims the issuer left out before storing it.
     */
    val issuance = strict.copy(requiredClaims = RequiredClaimsPolicy.RequireMandatory)

    /**
     * Requires the integrity of an artifact only: parsing, signatures, and bindings, e.g. for artifacts of test parties
     * that no trust list contains.
     */
    val lenient = ValidationPolicy(
        trust = TrustPolicy.IntegrityOnly,
        status = StatusPolicy.Skip,
        timeLeeway = timeLeeway,
        requireTimeliness = false,
    )
}
