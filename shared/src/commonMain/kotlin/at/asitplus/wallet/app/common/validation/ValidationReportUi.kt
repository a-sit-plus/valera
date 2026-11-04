package at.asitplus.wallet.app.common.validation

import at.asitplus.openid.OpenId4VciClaimsPathPointer
import at.asitplus.openid.OpenId4VciClaimsPathPointerSegmentString
import at.asitplus.wallet.lib.agent.validation.relyingParty.MissingRegistrationCertificateException
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.RequestDataValidity
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.WrpCredentialRequest
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.primitives.TokenStatus
import at.asitplus.wallet.lib.validation.AccessCertificateChecks
import at.asitplus.wallet.lib.validation.CheckOutcome
import at.asitplus.wallet.lib.validation.ClientAttestationChecks
import at.asitplus.wallet.lib.validation.CredentialChecks
import at.asitplus.wallet.lib.validation.IssuerMetadataChecks
import at.asitplus.wallet.lib.validation.JwtProofChecks
import at.asitplus.wallet.lib.validation.KeyAttestationChecks
import at.asitplus.wallet.lib.validation.MissingRequiredClaimException
import at.asitplus.wallet.lib.validation.NamedCheck
import at.asitplus.wallet.lib.validation.PresentationChecks
import at.asitplus.wallet.lib.validation.ProtocolResponseChecks
import at.asitplus.wallet.lib.validation.RegistrationCertificateChecks
import at.asitplus.wallet.lib.validation.RelyingPartyChecks
import at.asitplus.wallet.lib.validation.RequestObjectChecks
import at.asitplus.wallet.lib.validation.StatusListTokenChecks
import at.asitplus.wallet.lib.validation.StatusMechanismValidation
import at.asitplus.wallet.lib.validation.TimelinessException
import at.asitplus.wallet.lib.validation.TokenStatusException
import at.asitplus.wallet.lib.validation.UnregisteredRequestException
import at.asitplus.wallet.lib.validation.ValidationChecks
import at.asitplus.wallet.lib.validation.ValidationDecision
import at.asitplus.wallet.lib.validation.ValidationReport
import at.asitplus.wallet.lib.validation.cause
import at.asitplus.wallet.lib.validation.VerifierAttestationChecks
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * A [ValidationReport] as shown to the user: serializable, so that it can be passed in routes, and without the
 * exceptions of the report, which are turned into [FindingUi]s.
 */
@Serializable
data class ValidationOutcomeUi(
    /** Whether the report has been accepted under its policy, see [ValidationPolicies.strict]. */
    val accepted: Boolean,
    /**
     * Whether a check failed that the user must not continue past, e.g. a signature, as opposed to checks of trust,
     * time, status, and registration, which the wallet only warns about.
     */
    val integrityFailed: Boolean,
    /** The most important problems, integrity failures first, to summarize the report. */
    val headline: List<HeadlineUi>,
    val root: ArtifactUi,
    val evaluatedAtEpochMillis: Long,
)

/** A [check] of an artifact of [kind] that is not satisfied, see [ValidationOutcomeUi.headline]. */
@Serializable
data class HeadlineUi(
    val kind: ArtifactKind,
    val check: CheckUi,
)

/** The report of one artifact with the reports of the artifacts it contains, e.g. a WRPAC of a relying party. */
@Serializable
data class ArtifactUi(
    val kind: ArtifactKind,
    val accepted: Boolean,
    val checks: List<CheckUi>,
    val children: List<ArtifactUi>,
)

/** One [NamedCheck] of a report. */
@Serializable
data class CheckUi(
    /** The name of the check in VC-K, e.g. `trust` or `status.mechanisms[0]`. */
    val name: String,
    val state: CheckState,
    /** Whether the check does not reject the report, see [NamedCheck.satisfied]. */
    val satisfied: Boolean,
    /** Whether the user may continue although this check is not [satisfied], see [tolerable]. */
    val tolerable: Boolean,
    /** What went wrong, `null` for a check that passed or does not apply. */
    val finding: FindingUi? = null,
)

@Serializable
enum class CheckState {
    PASSED,
    FAILED,

    /** The check could not run, e.g. because trust anchors are missing or a status list is unavailable. */
    NOT_CHECKED,
    NOT_APPLICABLE,
}

/** The kind of artifact a report is about, one per [ValidationChecks] type. */
@Serializable
enum class ArtifactKind {
    CREDENTIAL,
    STATUS_LIST_TOKEN,
    PRESENTATION,
    PROTOCOL_RESPONSE,
    JWT_PROOF,
    KEY_ATTESTATION,
    CLIENT_ATTESTATION,
    REQUEST_OBJECT,
    VERIFIER_ATTESTATION,
    RELYING_PARTY,
    ACCESS_CERTIFICATE,
    REGISTRATION_CERTIFICATE,
    ISSUER_METADATA,
}

/** What went wrong in a check, in terms the user can understand. */
@Serializable
sealed interface FindingUi {
    /** The signer does not lead to an anchor of [source], e.g. a trust list. */
    @Serializable
    @SerialName("untrusted")
    data class Untrusted(val source: String?) : FindingUi

    /** Trust could not be decided, e.g. because there are no anchors of [source]. */
    @Serializable
    @SerialName("trust_not_checked")
    data class TrustNotChecked(val source: String?) : FindingUi

    @Serializable
    @SerialName("expired")
    data class Expired(val notAfterEpochMillis: Long?) : FindingUi

    @Serializable
    @SerialName("not_yet_valid")
    data class NotYetValid(val notBeforeEpochMillis: Long?) : FindingUi

    @Serializable
    @SerialName("revoked")
    data object Revoked : FindingUi

    @Serializable
    @SerialName("suspended")
    data object Suspended : FindingUi

    /** An application-specific status, which is not accepted either. */
    @Serializable
    @SerialName("other_status")
    data class OtherStatus(val value: Int) : FindingUi

    /** The status could not be obtained, e.g. because the status list is not reachable. */
    @Serializable
    @SerialName("status_unavailable")
    data class StatusUnavailable(val reason: String?) : FindingUi

    /** A registration certificate is not issued to the relying party of the access certificate. */
    /** The credential does not contain the [claims] its type requires, labeled for the user. */
    @Serializable
    @SerialName("missing_claims")
    data class MissingClaims(val claims: List<String>) : FindingUi

    @Serializable
    @SerialName("not_linked")
    data object NotLinked : FindingUi

    /** The relying party requests credentials or claims its registration does not cover. */
    @Serializable
    @SerialName("unregistered")
    data class Unregistered(val requests: List<UnregisteredRequestUi>) : FindingUi

    /** The request or metadata carry no registration information at all. */
    @Serializable
    @SerialName("missing_registration")
    data object MissingRegistration : FindingUi

    /** The metadata of a credential issuer are not signed, so the issuer can not be verified. */
    @Serializable
    @SerialName("unsigned")
    data object Unsigned : FindingUi

    /** The check could not run, see [reason]. */
    @Serializable
    @SerialName("not_checked")
    data class NotChecked(val reason: String?) : FindingUi

    @Serializable
    @SerialName("message")
    data class Message(val text: String) : FindingUi
}

/** A [request] of a relying party, with what of it is registered. */
@Serializable
data class UnregisteredRequestUi(
    val request: WrpCredentialRequest,
    val validity: RequestDataValidity,
)

/** This artifact and every artifact it contains, depth-first. */
fun ArtifactUi.flatten(): Sequence<ArtifactUi> = sequenceOf(this) + children.asSequence().flatMap { it.flatten() }

/**
 * The group a check is shown in: the parts of a compound check, e.g. `status.claim` and `status.mechanisms[0]`, or the
 * disclosed items of a credential, are shown as one.
 */
val CheckUi.group: String
    get() = name.substringBefore('.').substringBefore('[')

/**
 * The [ArtifactUi.checks] to show: one per [group], with the worst state and the first finding of its parts, without
 * checks that do not apply.
 */
fun ArtifactUi.displayedChecks(): List<CheckUi> = checks
    .groupBy { it.group }
    .map { (group, parts) ->
        parts.singleOrNull()?.copy(name = group) ?: CheckUi(
            name = group,
            state = parts.maxOf { it.state.severity }.let { severity -> CheckState.entries.first { it.severity == severity } },
            satisfied = parts.all { it.satisfied },
            tolerable = parts.all { it.tolerable },
            finding = parts.firstNotNullOfOrNull { it.finding },
        )
    }
    .filterNot { it.state == CheckState.NOT_APPLICABLE }

private val CheckState.severity: Int
    get() = when (this) {
        CheckState.NOT_APPLICABLE -> 0
        CheckState.PASSED -> 1
        CheckState.NOT_CHECKED -> 2
        CheckState.FAILED -> 3
    }

/** At most this many problems are summarized in [ValidationOutcomeUi.headline]. */
private const val HEADLINE_SIZE = 3

/**
 * Artifacts whose problems are always warnings, whatever the names of their checks: a WRPRC registers a relying party
 * or issuer, but does not authenticate a request, and a status list token only tells the status of the artifact
 * referencing it, which [isTolerable] already lets the user continue past.
 */
private val WARNING_ONLY_KINDS = setOf(ArtifactKind.REGISTRATION_CERTIFICATE, ArtifactKind.STATUS_LIST_TOKEN)

/**
 * This report as shown to the user, with the paths of claims a credential misses labeled by [claimLabel], e.g. from the
 * metadata of its type, by default the member names of the path.
 */
fun ValidationReport.toUi(
    claimLabel: (OpenId4VciClaimsPathPointer) -> String = { it.genericClaimLabel() },
): ValidationOutcomeUi {
    val root = toArtifactUi(claimLabel)
    val problems = root.problems(warningOnly = false)
    return ValidationOutcomeUi(
        accepted = decision == ValidationDecision.ACCEPTED,
        integrityFailed = problems.any { it.integrity },
        headline = problems.sortedByDescending { it.integrity }.take(HEADLINE_SIZE).map { it.headline },
        root = root,
        evaluatedAtEpochMillis = evaluatedAt.toEpochMilliseconds(),
    )
}

private class Problem(val headline: HeadlineUi, val integrity: Boolean)

/** Unsatisfied checks in depth-first order. */
private fun ArtifactUi.problems(warningOnly: Boolean): List<Problem> {
    val inWarningOnly = warningOnly || kind in WARNING_ONLY_KINDS
    return checks.filterNot { it.satisfied }.map {
        Problem(HeadlineUi(kind, it), integrity = !inWarningOnly && !it.tolerable)
    } + children.flatMap { it.problems(inWarningOnly) }
}

private fun ValidationReport.toArtifactUi(claimLabel: (OpenId4VciClaimsPathPointer) -> String): ArtifactUi = ArtifactUi(
    kind = checks.kind,
    accepted = decision == ValidationDecision.ACCEPTED,
    checks = namedChecks.map { it.toUi(evaluatedAt, checks, claimLabel) },
    children = children.map { it.toArtifactUi(claimLabel) } +
        namedChecks.mapNotNull { it.statusMechanism?.token?.toArtifactUi(claimLabel) },
)

/** The member names of this path, joined by dots, e.g. `address.country`. */
internal fun OpenId4VciClaimsPathPointer.genericClaimLabel(): String =
    mapNotNull { (it as? OpenId4VciClaimsPathPointerSegmentString)?.string }.joinToString(".").ifEmpty { toString() }

private val ValidationChecks.kind: ArtifactKind
    get() = when (this) {
        is CredentialChecks -> ArtifactKind.CREDENTIAL
        is StatusListTokenChecks -> ArtifactKind.STATUS_LIST_TOKEN
        is PresentationChecks -> ArtifactKind.PRESENTATION
        is ProtocolResponseChecks -> ArtifactKind.PROTOCOL_RESPONSE
        is JwtProofChecks -> ArtifactKind.JWT_PROOF
        is KeyAttestationChecks -> ArtifactKind.KEY_ATTESTATION
        is ClientAttestationChecks -> ArtifactKind.CLIENT_ATTESTATION
        is RequestObjectChecks -> ArtifactKind.REQUEST_OBJECT
        is VerifierAttestationChecks -> ArtifactKind.VERIFIER_ATTESTATION
        is RelyingPartyChecks -> ArtifactKind.RELYING_PARTY
        is AccessCertificateChecks -> ArtifactKind.ACCESS_CERTIFICATE
        is RegistrationCertificateChecks -> ArtifactKind.REGISTRATION_CERTIFICATE
        is IssuerMetadataChecks -> ArtifactKind.ISSUER_METADATA
    }

private fun NamedCheck.toUi(
    evaluatedAt: Instant,
    of: ValidationChecks,
    claimLabel: (OpenId4VciClaimsPathPointer) -> String,
) = CheckUi(
    name = name,
    state = when (outcome) {
        CheckOutcome.Passed -> CheckState.PASSED
        is CheckOutcome.Failed -> CheckState.FAILED
        is CheckOutcome.Blocked -> CheckState.NOT_CHECKED
        CheckOutcome.NotApplicable -> CheckState.NOT_APPLICABLE
    },
    satisfied = satisfied,
    tolerable = tolerable(of),
    finding = finding(evaluatedAt, claimLabel),
)

/**
 * Whether the user may continue past this check failing: trust, artifact time, status, registration, and the claims a
 * credential type requires, which the issuer may have left out. Relies on the
 * naming conventions of [NamedCheck]. Every other check blocks, e.g. `parsing`, every signature (`signature`,
 * `issuerSignature`, `popSignature`), `holderBinding`, `clientId`, `walletNonce`, `expectedOrigin`, `challenge`,
 * `nonce`, `audience`, `subject`, and the time of a proof of the holder (`proofTime`, `popTime`). So does a check VC-K
 * adds later, until it is classified here.
 */
internal fun NamedCheck.tolerable() =
    trust != null ||                        // issuerTrust, trust, signerTrust: carry their TrustValidation
        name == "timeliness" ||             // validity of the artifact, never the time of a holder proof
        name.startsWith("status.") ||       // status.claim, status.mechanisms[i], status.agreement
        name == REQUIRED_CLAIMS ||          // claims the type of a credential requires, carries the missing ones
        name in REGISTRATION_CHECKS         // registration of a relying party (WRPRC) or issuer (issuer_info)

private val REGISTRATION_CHECKS = setOf("linkage", "requestAuthorization", "registration")

/**
 * Whether the user may continue past this check of the artifact with the checks [of] failing, see [tolerable]. Also
 * the `parsing` of an access certificate that signed the request: its leaf carries no identifier of a relying party
 * (ETSI TS 119 475, 5.1.2 and 5.1.4), e.g. a reader certificate of another PKI, so the request is authenticated, only
 * not by a WRPAC, which is warned about like an untrusted one.
 */
internal fun NamedCheck.tolerable(of: ValidationChecks) =
    tolerable() || (name == "parsing" && of is AccessCertificateChecks && of.signature == CheckOutcome.Passed)

private fun NamedCheck.finding(
    evaluatedAt: Instant,
    claimLabel: (OpenId4VciClaimsPathPointer) -> String,
): FindingUi? {
    if (outcome == CheckOutcome.Passed || outcome == CheckOutcome.NotApplicable) return null
    val cause = outcome.cause()
    val blocked = outcome is CheckOutcome.Blocked
    trust?.let { return if (blocked) FindingUi.TrustNotChecked(it.source) else FindingUi.Untrusted(it.source) }
    statusMechanism?.let { return it.finding(cause) }
    return when {
        cause is MissingRequiredClaimException -> FindingUi.MissingClaims(cause.missing.map(claimLabel))
        cause is TimelinessException -> cause.finding(evaluatedAt)
        cause is TokenStatusException -> cause.status.finding()
        cause is MissingRegistrationCertificateException -> FindingUi.MissingRegistration
        cause is UnregisteredRequestException ->
            FindingUi.Unregistered(cause.requests.map { (request, validity) -> UnregisteredRequestUi(request, validity) })

        name == "linkage" && !blocked -> FindingUi.NotLinked
        blocked -> FindingUi.NotChecked(cause?.displayText())
        else -> FindingUi.Message(cause?.displayText() ?: name)
    }
}

private fun StatusMechanismValidation.finding(cause: Throwable?): FindingUi =
    (cause as? TokenStatusException)?.status?.finding()
        ?: observed?.takeIf { it != TokenStatus.Valid }?.finding()
        ?: FindingUi.StatusUnavailable(
            (token.namedChecks.firstOrNull { !it.satisfied }?.outcome?.cause() ?: cause)?.displayText()
        )

private fun TokenStatus.finding(): FindingUi = when (this) {
    TokenStatus.Invalid -> FindingUi.Revoked
    TokenStatus.Suspended -> FindingUi.Suspended
    else -> FindingUi.OtherStatus(value.toInt())
}

private fun TimelinessException.finding(evaluatedAt: Instant): FindingUi {
    val notAfter = notAfter
    val notBefore = notBefore
    return when {
        notAfter != null && evaluatedAt > notAfter -> FindingUi.Expired(notAfter.toEpochMilliseconds())
        notBefore != null && evaluatedAt < notBefore -> FindingUi.NotYetValid(notBefore.toEpochMilliseconds())
        else -> FindingUi.Message(displayText())
    }
}

/**
 * Why [this] report fails its integrity, see [ValidationOutcomeUi.integrityFailed]: its first check, depth-first, that
 * is not satisfied and that the user may not continue past, with the cause of its outcome, `null` if there is none.
 * Unlike [rejection], it does not name a check the user may continue past, e.g. trust, which may come first. A check
 * that failed is named before one that could not be checked because of it, e.g. the `signature` of an access
 * certificate before its `parsing`, which is blocked without the chain of a signature that does not verify.
 */
fun ValidationReport.integrityRejection(): Throwable? = integrityRejection(warningOnly = false)

private fun ValidationReport.integrityRejection(warningOnly: Boolean): Throwable? {
    val inWarningOnly = warningOnly || checks.kind in WARNING_ONLY_KINDS
    if (!inWarningOnly) {
        val rejecting = namedChecks.filter { !it.satisfied && !it.tolerable(checks) }
        (rejecting.firstOrNull { it.outcome is CheckOutcome.Failed } ?: rejecting.firstOrNull())?.let {
            val cause = it.outcome.cause()
            val message = "The ${it.name} check of the ${checks::class.simpleName} failed" +
                (cause?.let { cause -> ": ${cause.displayText()}" } ?: "")
            return IllegalArgumentException(message, cause)
        }
    }
    return children.firstNotNullOfOrNull { it.integrityRejection(inWarningOnly) }
}

/**
 * Whether the user has to decide about [this] rejected report of an issued credential: not if its only problem is that
 * the claims its type requires could not be checked, e.g. because the wallet knows no metadata of the type, which is no
 * fault of the issuer. Claims the issuer left out, see [FindingUi.MissingClaims], are reviewed.
 */
fun ValidationReport.needsReview(): Boolean =
    namedChecks.any { !it.satisfied && !(it.name == REQUIRED_CLAIMS && it.outcome is CheckOutcome.Blocked) } ||
        children.any { it.decision != ValidationDecision.ACCEPTED }

private const val REQUIRED_CLAIMS = "requiredClaims"

/**
 * Every check of [this] report and its children that is not satisfied, depth-first, with the cause of its outcome,
 * e.g. to log why an artifact has been rejected.
 */
fun ValidationReport.describeProblems(): String = problemDescriptions().joinToString("; ").ifEmpty { "none" }

private fun ValidationReport.problemDescriptions(): List<String> =
    namedChecks.filterNot { it.satisfied }.map {
        "${checks::class.simpleName}.${it.name}: ${it.outcome.cause()?.displayText() ?: it.outcome}"
    } + children.flatMap { it.problemDescriptions() }

/**
 * Why [this] report is rejected: the cause of its first check that is not satisfied, otherwise why its first rejected
 * child is rejected, `null` if it is accepted.
 */
fun ValidationReport.rejection(): Throwable? =
    if (decision == ValidationDecision.ACCEPTED) null
    else namedChecks.firstOrNull { !it.satisfied }
        ?.let { it.outcome.cause() ?: IllegalArgumentException("The ${it.name} check did not pass") }
        ?: children.firstNotNullOfOrNull { it.rejection() }
        ?: IllegalArgumentException("The ${checks::class.simpleName} are rejected")

/** Messages of this exception and its causes, e.g. the parsing error that caused a validation to fail. */
internal fun Throwable.displayText(): String = generateSequence(this) { it.cause }
    .mapNotNull { it.message }
    .distinct()
    .joinToString(": ")
    .ifEmpty { this::class.simpleName ?: "Unknown error" }
