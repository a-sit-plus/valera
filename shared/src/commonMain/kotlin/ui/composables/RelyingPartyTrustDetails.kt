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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import at.asitplus.valera.resources.text_label_subject
import at.asitplus.valera.resources.text_label_thumbprint
import at.asitplus.valera.resources.text_label_valid_from
import at.asitplus.valera.resources.text_label_valid_to
import at.asitplus.valera.resources.trust_certificate_details
import at.asitplus.valera.resources.trust_certificate_details_description
import at.asitplus.valera.resources.trust_signer
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
import org.jetbrains.compose.resources.stringResource

@Composable
fun RelyingPartyDataDisplaySection(
    serviceProviderName: String?,
    serviceProviderLocation: String?,
    trustResult: RelyingPartyTrustResult,
    modifier: Modifier = Modifier,
) {
    var signerDetailsExpanded by remember(trustResult.signers) { mutableStateOf(false) }
    val hasSignerDetails = trustResult.signers.isNotEmpty()
    val signerDetailsActionDescription = if (signerDetailsExpanded) {
        stringResource(Res.string.trust_signer_hide_details)
    } else {
        stringResource(Res.string.trust_signer_show_details)
    }

    DataDisplaySection(
        title = stringResource(Res.string.section_heading_data_recipient),
        modifier = modifier,
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
            TrustStatusBanner(
                trustState = trustResult.summary.bannerState,
                text = trustResult.summary.summaryText(trustResult.signers.isNotEmpty()),
                modifier = Modifier.padding(bottom = 16.dp),
                onClick = if (hasSignerDetails) {
                    { signerDetailsExpanded = !signerDetailsExpanded }
                } else {
                    null
                },
                onClickLabel = signerDetailsActionDescription.takeIf { hasSignerDetails },
                trailingContent = if (hasSignerDetails) {
                    {
                        Icon(
                            imageVector = if (signerDetailsExpanded) {
                                Icons.Filled.ArrowDropUp
                            } else {
                                Icons.Filled.ArrowDropDown
                            },
                            contentDescription = null,
                        )
                    }
                } else {
                    null
                },
            )
            if (signerDetailsExpanded) {
                RelyingPartySignerDetails(trustResult.signers)
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

    signers.forEach { signer ->
        val displayId = signer.clientId
            ?: stringResource(Res.string.trust_signer_fallback, signer.signatureIndex + 1)
        SignerTrustCard(
            signer = signer,
            displayId = displayId,
            onShowCertificate = { certificate -> selectedCertificate = displayId to certificate },
        )
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
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
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
                TrustStatusBanner(
                    trustState = signer.trustState,
                    text = stringResource(signer.trustState.displayVerifierText),
                    modifier = Modifier.padding(top = 10.dp),
                )
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
