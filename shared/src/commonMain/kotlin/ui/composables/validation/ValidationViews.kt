package ui.composables.validation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.validation_artifact_access_certificate
import at.asitplus.valera.resources.validation_artifact_client_attestation
import at.asitplus.valera.resources.validation_artifact_credential
import at.asitplus.valera.resources.validation_artifact_issuer_metadata
import at.asitplus.valera.resources.validation_artifact_jwt_proof
import at.asitplus.valera.resources.validation_artifact_key_attestation
import at.asitplus.valera.resources.validation_artifact_presentation
import at.asitplus.valera.resources.validation_artifact_protocol_response
import at.asitplus.valera.resources.validation_artifact_registration_certificate
import at.asitplus.valera.resources.validation_artifact_relying_party
import at.asitplus.valera.resources.validation_artifact_request_object
import at.asitplus.valera.resources.validation_artifact_status_list_token
import at.asitplus.valera.resources.validation_artifact_verifier_attestation
import at.asitplus.valera.resources.validation_check_client_id
import at.asitplus.valera.resources.validation_check_disclosed_items
import at.asitplus.valera.resources.validation_check_expected_origin
import at.asitplus.valera.resources.validation_check_holder_binding
import at.asitplus.valera.resources.validation_check_issuer_signature
import at.asitplus.valera.resources.validation_check_issuer_trust
import at.asitplus.valera.resources.validation_check_linkage
import at.asitplus.valera.resources.validation_check_parsing
import at.asitplus.valera.resources.validation_check_registration
import at.asitplus.valera.resources.validation_check_request_authorization
import at.asitplus.valera.resources.validation_check_request_data
import at.asitplus.valera.resources.validation_check_required_claims
import at.asitplus.valera.resources.validation_check_retrieval
import at.asitplus.valera.resources.validation_check_semantics
import at.asitplus.valera.resources.validation_check_signature
import at.asitplus.valera.resources.validation_check_signer_trust
import at.asitplus.valera.resources.validation_check_status
import at.asitplus.valera.resources.validation_check_subject
import at.asitplus.valera.resources.validation_check_timeliness
import at.asitplus.valera.resources.validation_check_trust
import at.asitplus.valera.resources.validation_check_wallet_nonce
import at.asitplus.valera.resources.validation_details
import at.asitplus.valera.resources.validation_finding_expired
import at.asitplus.valera.resources.validation_finding_expired_at
import at.asitplus.valera.resources.validation_finding_missing_claims
import at.asitplus.valera.resources.validation_finding_missing_registration
import at.asitplus.valera.resources.validation_finding_not_checked
import at.asitplus.valera.resources.validation_finding_not_checked_reason
import at.asitplus.valera.resources.validation_finding_not_linked
import at.asitplus.valera.resources.validation_finding_not_yet_valid
import at.asitplus.valera.resources.validation_finding_not_yet_valid_at
import at.asitplus.valera.resources.validation_finding_other_status
import at.asitplus.valera.resources.validation_finding_revoked
import at.asitplus.valera.resources.validation_finding_skipped
import at.asitplus.valera.resources.validation_finding_status_unavailable
import at.asitplus.valera.resources.validation_finding_status_unavailable_reason
import at.asitplus.valera.resources.validation_finding_suspended
import at.asitplus.valera.resources.validation_finding_trust_not_checked
import at.asitplus.valera.resources.validation_finding_unregistered
import at.asitplus.valera.resources.validation_finding_unsigned
import at.asitplus.valera.resources.validation_finding_untrusted
import at.asitplus.valera.resources.validation_finding_untrusted_source
import at.asitplus.valera.resources.validation_headline
import at.asitplus.valera.resources.validation_summary_failed
import at.asitplus.valera.resources.validation_summary_passed
import at.asitplus.valera.resources.validation_summary_warning
import at.asitplus.wallet.app.common.validation.ArtifactKind
import at.asitplus.wallet.app.common.validation.ArtifactUi
import at.asitplus.wallet.app.common.validation.CheckState
import at.asitplus.wallet.app.common.validation.CheckUi
import at.asitplus.wallet.app.common.validation.FindingUi
import at.asitplus.wallet.app.common.validation.HeadlineUi
import at.asitplus.wallet.app.common.validation.ValidationOutcomeUi
import at.asitplus.wallet.app.common.validation.displayedChecks
import at.asitplus.wallet.app.common.validation.group
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ui.composables.ExpandableCard
import ui.theme.LocalExtendedColors
import kotlin.time.Instant

/** The verdict of a validation, with its most important problems. */
@Composable
fun ValidationBanner(outcome: ValidationOutcomeUi, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val (background, content) = when {
        outcome.accepted -> colors.primaryContainer to colors.onPrimaryContainer
        outcome.integrityFailed -> colors.errorContainer to colors.onErrorContainer
        else -> colors.tertiaryContainer to colors.onTertiaryContainer
    }
    val title = when {
        outcome.accepted -> Res.string.validation_summary_passed
        outcome.integrityFailed -> Res.string.validation_summary_failed
        else -> Res.string.validation_summary_warning
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (outcome.accepted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = null,
                tint = content,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(title), color = content, style = MaterialTheme.typography.titleSmall)
        }
        outcome.headline.forEach {
            Text(
                text = it.text(),
                color = content,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 32.dp, top = 4.dp),
            )
        }
    }
}

/** Every check of a validation, per artifact, expanded if the validation did not pass. */
@Composable
fun ValidationDetails(outcome: ValidationOutcomeUi) {
    ExpandableCard(
        text = stringResource(Res.string.validation_details),
        icon = Icons.Outlined.Info,
        expanded = !outcome.accepted,
    ) {
        ArtifactDetails(outcome.root, depth = 0)
    }
}

@Composable
private fun ArtifactDetails(artifact: ArtifactUi, depth: Int) {
    Column(modifier = Modifier.padding(start = (depth * 12).dp, top = 8.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StateIcon(
                icon = if (artifact.accepted) Icons.Outlined.Check else Icons.Outlined.Clear,
                tint = if (artifact.accepted) validColor() else invalidColor(),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(artifact.kind.label), style = MaterialTheme.typography.titleSmall)
        }
        artifact.displayedChecks().forEach { CheckRow(it) }
        artifact.children.forEach { ArtifactDetails(it, depth + 1) }
    }
}

/**
 * A check, marked as passed, failed, not checked on its own, e.g. without a reachable status list, or skipped because
 * other checks failed, which is shown in a muted color to not count as a problem of its own.
 */
@Composable
private fun CheckRow(check: CheckUi) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val (icon, tint) = when {
        check.state == CheckState.PASSED -> Icons.Outlined.Check to validColor()
        check.state == CheckState.SKIPPED -> SkippedIcon to muted
        check.satisfied -> Icons.Outlined.Info to muted
        check.state == CheckState.NOT_CHECKED -> Icons.Outlined.Warning to warningColor()
        else -> Icons.Outlined.Clear to invalidColor()
    }
    val textColor = if (check.state == CheckState.SKIPPED) muted else Color.Unspecified
    Row(modifier = Modifier.padding(start = 8.dp, top = 4.dp)) {
        StateIcon(icon, tint)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = check.label(), color = textColor, style = MaterialTheme.typography.bodyMedium)
            check.finding?.let {
                Text(text = it.text(check), color = textColor, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** A dash, i.e. Material's "Remove" icon, which is not part of the core icons. */
private val SkippedIcon: ImageVector = materialIcon(name = "Outlined.Remove") {
    materialPath {
        moveTo(19f, 13f)
        horizontalLineTo(5f)
        verticalLineToRelative(-2f)
        horizontalLineToRelative(14f)
        verticalLineToRelative(2f)
        close()
    }
}

@Composable
private fun StateIcon(icon: ImageVector, tint: Color) {
    Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
}

@Composable
private fun validColor() = LocalExtendedColors.current.validationDark.valid

@Composable
private fun invalidColor() = LocalExtendedColors.current.validationDark.invalid

@Composable
private fun warningColor() = LocalExtendedColors.current.validationDark.warning

/** A sentence for a problem in the headline, e.g. "Access certificate: Not trusted by WRPAC providers". */
@Composable
fun HeadlineUi.text(): String = stringResource(
    Res.string.validation_headline,
    stringResource(kind.label),
    check.finding?.text(check) ?: check.label(),
)

@Composable
fun FindingUi.text(check: CheckUi): String = when (this) {
    is FindingUi.Untrusted -> source?.let { stringResource(Res.string.validation_finding_untrusted_source, it) }
        ?: stringResource(Res.string.validation_finding_untrusted)

    is FindingUi.TrustNotChecked -> stringResource(Res.string.validation_finding_trust_not_checked)
    is FindingUi.Expired -> notAfterEpochMillis?.let {
        stringResource(Res.string.validation_finding_expired_at, it.formatDate())
    } ?: stringResource(Res.string.validation_finding_expired)

    is FindingUi.NotYetValid -> notBeforeEpochMillis?.let {
        stringResource(Res.string.validation_finding_not_yet_valid_at, it.formatDate())
    } ?: stringResource(Res.string.validation_finding_not_yet_valid)

    FindingUi.Revoked -> stringResource(Res.string.validation_finding_revoked)
    FindingUi.Suspended -> stringResource(Res.string.validation_finding_suspended)
    is FindingUi.OtherStatus -> stringResource(Res.string.validation_finding_other_status, value.toString())
    is FindingUi.StatusUnavailable -> reason?.let { stringResource(Res.string.validation_finding_status_unavailable_reason, it) }
        ?: stringResource(Res.string.validation_finding_status_unavailable)

    is FindingUi.MissingClaims -> stringResource(Res.string.validation_finding_missing_claims, claims.joinToString(", "))
    FindingUi.NotLinked -> stringResource(Res.string.validation_finding_not_linked)
    is FindingUi.Unregistered -> stringResource(Res.string.validation_finding_unregistered)
    FindingUi.MissingRegistration -> stringResource(Res.string.validation_finding_missing_registration)
    FindingUi.Unsigned -> stringResource(Res.string.validation_finding_unsigned)
    is FindingUi.NotChecked -> reason?.let { stringResource(Res.string.validation_finding_not_checked_reason, it) }
        ?: stringResource(Res.string.validation_finding_not_checked)

    FindingUi.Skipped -> stringResource(Res.string.validation_finding_skipped)
    is FindingUi.Message -> "${check.label()}: $text"
}

private fun Long.formatDate(): String =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()

val ArtifactKind.label: StringResource
    get() = when (this) {
        ArtifactKind.CREDENTIAL -> Res.string.validation_artifact_credential
        ArtifactKind.STATUS_LIST_TOKEN -> Res.string.validation_artifact_status_list_token
        ArtifactKind.PRESENTATION -> Res.string.validation_artifact_presentation
        ArtifactKind.PROTOCOL_RESPONSE -> Res.string.validation_artifact_protocol_response
        ArtifactKind.JWT_PROOF -> Res.string.validation_artifact_jwt_proof
        ArtifactKind.KEY_ATTESTATION -> Res.string.validation_artifact_key_attestation
        ArtifactKind.CLIENT_ATTESTATION -> Res.string.validation_artifact_client_attestation
        ArtifactKind.REQUEST_OBJECT -> Res.string.validation_artifact_request_object
        ArtifactKind.VERIFIER_ATTESTATION -> Res.string.validation_artifact_verifier_attestation
        ArtifactKind.RELYING_PARTY -> Res.string.validation_artifact_relying_party
        ArtifactKind.ACCESS_CERTIFICATE -> Res.string.validation_artifact_access_certificate
        ArtifactKind.REGISTRATION_CERTIFICATE -> Res.string.validation_artifact_registration_certificate
        ArtifactKind.ISSUER_METADATA -> Res.string.validation_artifact_issuer_metadata
    }

/** The label of the [group] of this check, its VC-K name for checks the wallet does not expect. */
@Composable
fun CheckUi.label(): String = when (group) {
    "parsing" -> Res.string.validation_check_parsing
    "signature" -> Res.string.validation_check_signature
    "trust" -> Res.string.validation_check_trust
    "issuerSignature" -> Res.string.validation_check_issuer_signature
    "issuerTrust" -> Res.string.validation_check_issuer_trust
    "signerTrust" -> Res.string.validation_check_signer_trust
    "semantics" -> Res.string.validation_check_semantics
    "disclosedItems" -> Res.string.validation_check_disclosed_items
    "requiredClaims" -> Res.string.validation_check_required_claims
    "holderBinding" -> Res.string.validation_check_holder_binding
    "timeliness" -> Res.string.validation_check_timeliness
    "status" -> Res.string.validation_check_status
    "clientId" -> Res.string.validation_check_client_id
    "walletNonce" -> Res.string.validation_check_wallet_nonce
    "expectedOrigin" -> Res.string.validation_check_expected_origin
    "requestData" -> Res.string.validation_check_request_data
    "linkage" -> Res.string.validation_check_linkage
    "requestAuthorization" -> Res.string.validation_check_request_authorization
    "subject" -> Res.string.validation_check_subject
    "registration" -> Res.string.validation_check_registration
    "retrieval" -> Res.string.validation_check_retrieval
    else -> null
}?.let { stringResource(it) } ?: name
