package ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import at.asitplus.signum.indispensable.Digest
import at.asitplus.signum.indispensable.asn1.encoding.decodeToString
import at.asitplus.signum.indispensable.pki.AttributeTypeAndValue
import at.asitplus.signum.indispensable.pki.X509Certificate
import at.asitplus.signum.supreme.hash.digest
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.attribute_friendly_name_data_recipient_location
import at.asitplus.valera.resources.attribute_friendly_name_data_recipient_name
import at.asitplus.valera.resources.button_label_close
import at.asitplus.valera.resources.section_heading_data_recipient
import at.asitplus.valera.resources.section_heading_trust
import at.asitplus.valera.resources.text_label_subject
import at.asitplus.valera.resources.text_label_thumbprint
import at.asitplus.valera.resources.text_label_valid_from
import at.asitplus.valera.resources.text_label_valid_to
import at.asitplus.valera.resources.trust_certificate_details
import at.asitplus.valera.resources.trust_certificate_details_description
import at.asitplus.valera.resources.trust_signer
import at.asitplus.valera.resources.signature_status_invalid
import at.asitplus.valera.resources.signature_status_invalid_summary
import at.asitplus.valera.resources.signature_status_unsupported
import at.asitplus.valera.resources.signature_status_untrusted
import at.asitplus.valera.resources.trust_signer_fallback
import at.asitplus.valera.resources.trust_signer_hide_details
import at.asitplus.valera.resources.trust_signer_show_details
import at.asitplus.valera.resources.trust_status_all_signers_trusted
import at.asitplus.valera.resources.trust_status_all_signers_untrusted
import at.asitplus.valera.resources.trust_status_evaluating_verifier
import at.asitplus.valera.resources.trust_status_no_signer_trusted_unknown
import at.asitplus.valera.resources.trust_status_signers_unknown
import at.asitplus.valera.resources.trust_status_some_signers_trusted
import at.asitplus.valera.resources.trust_status_trusted_verifier
import at.asitplus.valera.resources.trust_status_unknown_verifier
import at.asitplus.valera.resources.trust_status_untrusted_verifier
import at.asitplus.wallet.app.common.RelyingPartySignerTrust
import at.asitplus.wallet.app.common.RelyingPartyTrustResult
import at.asitplus.wallet.app.common.RelyingPartyTrustSummary
import at.asitplus.wallet.lib.openid.VerifierSignature
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun RelyingPartyDataDisplaySection(
    serviceProviderName: String?,
    serviceProviderLocation: String?,
    trustResult: RelyingPartyTrustResult,
    modifier: Modifier = Modifier,
) {
    val hasSignerDetails = trustResult.signers.isNotEmpty()

    Column(modifier = modifier) {
        DataDisplaySection(
            title = stringResource(Res.string.section_heading_data_recipient),
        ) {
            Column(modifier = Modifier.padding(start = 32.dp)) {
                serviceProviderName?.let {
                    LabeledText(
                        label = stringResource(Res.string.attribute_friendly_name_data_recipient_name),
                        text = it,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }
                serviceProviderLocation?.takeIf { it.isNotBlank() }?.let {
                    LabeledText(
                        label = stringResource(Res.string.attribute_friendly_name_data_recipient_location),
                        text = it,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }
            }
        }
        // Its cards span the section, like the one of the request validation
        DataDisplaySection(
            title = stringResource(Res.string.section_heading_trust),
        ) {
            Column {
                // A request-level problem, so it is shown without expanding, and apart from the trust decision
                val invalidSignatures = trustResult.signers.count {
                    it.signatureStatus == VerifierSignature.Status.INVALID
                }
                if (invalidSignatures > 0) {
                    SignatureStatusBanner(
                        status = VerifierSignature.Status.INVALID,
                        text = stringResource(
                            Res.string.signature_status_invalid_summary,
                            invalidSignatures,
                            trustResult.signers.size,
                        ),
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                val trustState = trustResult.summary.bannerState
                val (containerColor, contentColor) = trustState.statusColors()
                // Keyed by the signers, so that the card starts collapsed again once they are evaluated
                key(trustResult.signers) {
                    ExpandableCard(
                        text = trustResult.summary.summaryText(hasSignerDetails),
                        icon = trustState.statusIcon,
                        expanded = false,
                        containerColor = containerColor,
                        contentColor = contentColor,
                        expandLabel = stringResource(Res.string.trust_signer_show_details),
                        collapseLabel = stringResource(Res.string.trust_signer_hide_details),
                        content = if (hasSignerDetails) {
                            { RelyingPartySignerDetails(trustResult.signers) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RelyingPartyTrustSummary.summaryText(isMultiSigned: Boolean): String = stringResource(
    when (this) {
        RelyingPartyTrustSummary.EVALUATING -> Res.string.trust_status_evaluating_verifier
        RelyingPartyTrustSummary.TRUSTED -> if (isMultiSigned) {
            Res.string.trust_status_all_signers_trusted
        } else {
            Res.string.trust_status_trusted_verifier
        }
        RelyingPartyTrustSummary.UNTRUSTED -> if (isMultiSigned) {
            Res.string.trust_status_all_signers_untrusted
        } else {
            Res.string.trust_status_untrusted_verifier
        }
        RelyingPartyTrustSummary.UNKNOWN -> if (isMultiSigned) {
            Res.string.trust_status_signers_unknown
        } else {
            Res.string.trust_status_unknown_verifier
        }
        RelyingPartyTrustSummary.MIXED_WITH_TRUSTED -> Res.string.trust_status_some_signers_trusted
        RelyingPartyTrustSummary.MIXED_WITHOUT_TRUSTED -> Res.string.trust_status_no_signer_trusted_unknown
    }
)

private val RelyingPartyTrustSummary.bannerState: TrustState
    get() = when (this) {
        RelyingPartyTrustSummary.EVALUATING -> TrustState.EVALUATING
        RelyingPartyTrustSummary.TRUSTED -> TrustState.TRUSTED
        RelyingPartyTrustSummary.UNTRUSTED -> TrustState.UNTRUSTED
        RelyingPartyTrustSummary.UNKNOWN,
        RelyingPartyTrustSummary.MIXED_WITH_TRUSTED -> TrustState.UNKNOWN
        RelyingPartyTrustSummary.MIXED_WITHOUT_TRUSTED -> TrustState.UNTRUSTED
    }

@Composable
private fun RelyingPartySignerDetails(signers: List<RelyingPartySignerTrust>) {
    var selectedCertificate by remember { mutableStateOf<Pair<String, X509Certificate>?>(null) }

    // Indented like the rows of the registration certificate card
    Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 12.dp, bottom = 12.dp)) {
        signers.forEachIndexed { index, signer ->
            val displayId = signer.clientId
                ?: stringResource(Res.string.trust_signer_fallback, signer.signatureIndex + 1)
            SignerTrustCard(
                signer = signer,
                displayId = displayId,
                onShowCertificate = { certificate -> selectedCertificate = displayId to certificate },
                modifier = Modifier.padding(bottom = if (index < signers.lastIndex) 8.dp else 0.dp),
            )
        }
    }

    selectedCertificate?.let { (signerId, certificate) ->
        CertificateDetailsDialog(
            signerId = signerId,
            certificate = certificate,
            onDismiss = { selectedCertificate = null },
        )
    }
}

@Composable
private fun SignerTrustCard(
    signer: RelyingPartySignerTrust,
    displayId: String,
    onShowCertificate: (X509Certificate) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.trust_signer),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = displayId,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val trustState = signer.trustState
                if (signer.authenticated && trustState != null) {
                    TrustStatusBanner(
                        trustState = trustState,
                        text = stringResource(trustState.displayVerifierText),
                        modifier = Modifier.padding(top = 10.dp),
                    )
                } else {
                    // the certificate of a signer that did not sign is irrelevant, whatever it would evaluate to
                    SignatureStatusBanner(
                        status = signer.signatureStatus,
                        text = stringResource(signer.signatureStatus.displayText),
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
            signer.certificate?.let { certificate ->
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { onShowCertificate(certificate) }) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = stringResource(
                            Res.string.trust_certificate_details_description,
                            displayId,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Whether a signature authenticates its signer, kept apart from [TrustStatusBanner] but in the same design. An invalid
 * signature is a fault of the request, so it is red, unlike any trust decision.
 */
@Composable
private fun SignatureStatusBanner(
    status: VerifierSignature.Status,
    text: String,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val (backgroundColor, contentColor, icon) = when (status) {
        VerifierSignature.Status.INVALID ->
            Triple(colorScheme.errorContainer, colorScheme.onErrorContainer, Icons.Filled.Close)
        // rejected by the wallet's trust configuration, i.e. a trust decision
        VerifierSignature.Status.UNTRUSTED ->
            Triple(colorScheme.tertiaryContainer, colorScheme.onTertiaryContainer, Icons.Filled.Warning)
        VerifierSignature.Status.UNSUPPORTED,
        VerifierSignature.Status.AUTHENTICATED ->
            Triple(colorScheme.surfaceVariant, colorScheme.onSurfaceVariant, Icons.Outlined.Info)
    }
    StatusBanner(
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        icon = icon,
        text = text,
        modifier = modifier,
    )
}

private val VerifierSignature.Status.displayText: StringResource
    get() = when (this) {
        VerifierSignature.Status.INVALID -> Res.string.signature_status_invalid
        VerifierSignature.Status.UNSUPPORTED -> Res.string.signature_status_unsupported
        VerifierSignature.Status.UNTRUSTED -> Res.string.signature_status_untrusted
        // only non-authenticated statuses are displayed as a signature status
        VerifierSignature.Status.AUTHENTICATED -> Res.string.signature_status_unsupported
    }

@Composable
private fun CertificateDetailsDialog(
    signerId: String,
    certificate: X509Certificate,
    onDismiss: () -> Unit,
) {
    val details = remember(certificate) { certificate.toDisplayData() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.trust_certificate_details)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                LabeledText(
                    label = stringResource(Res.string.trust_signer),
                    text = signerId,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                LabeledText(
                    label = stringResource(Res.string.text_label_subject),
                    text = details.subject,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                LabeledContent(
                    label = stringResource(Res.string.text_label_thumbprint),
                    modifier = Modifier.padding(bottom = 16.dp),
                ) {
                    Text(details.sha256Thumbprint)
                }
                LabeledText(
                    label = stringResource(Res.string.text_label_valid_from),
                    text = details.validFrom,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                LabeledText(
                    label = stringResource(Res.string.text_label_valid_to),
                    text = details.validUntil,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.button_label_close))
            }
        },
    )
}

internal data class CertificateDisplayData(
    val subject: String,
    val sha256Thumbprint: String,
    val validFrom: String,
    val validUntil: String,
)

internal fun X509Certificate.toDisplayData(): CertificateDisplayData {
    val rdnAttributes = tbsCertificate.subjectName.flatMap { it.attrsAndValues }
    val subject = rdnAttributes
        .firstOrNull { it is AttributeTypeAndValue.Organization }
        ?.value?.asPrimitive()?.decodeToString()
        ?: rdnAttributes.firstOrNull { it is AttributeTypeAndValue.CommonName }
            ?.value?.asPrimitive()?.decodeToString()
        ?: tbsCertificate.subjectName.toString()
    val thumbprint = Digest.SHA256.digest(encodeToDer())
        .toHexString(HexFormat.UpperCase)
        .chunked(2)
        .joinToString(":")

    return CertificateDisplayData(
        subject = subject,
        sha256Thumbprint = thumbprint,
        validFrom = tbsCertificate.validFrom.instant.toString(),
        validUntil = tbsCertificate.validUntil.instant.toString(),
    )
}
