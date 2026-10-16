package at.asitplus.wallet.app.common.validation

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
     * Requires the integrity of an artifact only: parsing, signatures, and bindings. Used to store a credential the
     * user decided to keep, although [strict] rejects it.
     */
    val lenient = ValidationPolicy(
        trust = TrustPolicy.IntegrityOnly,
        status = StatusPolicy.Skip,
        timeLeeway = timeLeeway,
        requireTimeliness = false,
    )
}
