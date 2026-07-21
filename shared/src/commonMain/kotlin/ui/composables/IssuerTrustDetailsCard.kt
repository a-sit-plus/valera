package ui.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import at.asitplus.signum.indispensable.Digest
import at.asitplus.signum.indispensable.asn1.encoding.decodeToString
import at.asitplus.signum.indispensable.pki.AttributeTypeAndValue
import at.asitplus.signum.indispensable.pki.X509Certificate
import at.asitplus.signum.supreme.hash.digest
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_hide_technical_details
import at.asitplus.valera.resources.button_label_show_technical_details
import at.asitplus.valera.resources.section_heading_issuer_trust
import at.asitplus.valera.resources.section_heading_issuer_trust_icon_text
import at.asitplus.valera.resources.text_label_issued_by
import at.asitplus.valera.resources.text_label_thumbprint
import at.asitplus.valera.resources.text_label_trust_status
import at.asitplus.valera.resources.text_label_valid_from
import at.asitplus.valera.resources.text_label_valid_to
import at.asitplus.valera.resources.trust_status_evaluating
import at.asitplus.valera.resources.trust_status_trusted
import at.asitplus.valera.resources.trust_status_unknown
import at.asitplus.valera.resources.trust_status_untrusted
import org.jetbrains.compose.resources.stringResource

@Composable
fun IssuerTrustDetailsCard(
    certificate: X509Certificate,
    trustState: TrustState,
    modifier: Modifier = Modifier
) {
    var showTechnicalDetails by remember { mutableStateOf(false) }
    val contentPadding = Modifier.padding(bottom = 16.dp, end = 16.dp, start = 16.dp)


    val rdnAttributes = certificate.tbsCertificate.subjectName.flatMap { it.attrsAndValues }

    val issuerName = rdnAttributes.firstOrNull { it is AttributeTypeAndValue.Organization }?.value?.asPrimitive()?.decodeToString()
        ?: rdnAttributes.firstOrNull { it is AttributeTypeAndValue.CommonName }?.value?.asPrimitive()?.decodeToString()
        ?: "Unknown Issuer"


    val validFrom = certificate.tbsCertificate.validFrom.instant.toString()

    val validUntil = certificate.tbsCertificate.validUntil.instant.toString()

    val digest: String = Digest.SHA256.digest(certificate.encodeToDer()).contentToString()

    val displayTrust = when (trustState) {
        TrustState.TRUSTED -> stringResource(Res.string.trust_status_trusted)
        TrustState.UNTRUSTED -> stringResource(Res.string.trust_status_untrusted)
        TrustState.UNKNOWN -> stringResource(Res.string.trust_status_unknown)
        TrustState.EVALUATING -> stringResource(Res.string.trust_status_evaluating)
    }

    Column(modifier = modifier) {
        ElevatedCard(modifier = Modifier.padding(bottom = 16.dp)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                PersonAttributeDetailCardHeading(
                    iconText = stringResource(Res.string.section_heading_issuer_trust_icon_text),
                    title = stringResource(Res.string.section_heading_issuer_trust),
                )

                LabeledText(
                    label = stringResource(Res.string.text_label_issued_by),
                    text = issuerName,
                    modifier = contentPadding,
                    fontWeight = FontWeight.Normal
                )

                LabeledText(
                    label = stringResource(Res.string.text_label_trust_status),
                    text = displayTrust,
                    modifier = contentPadding,
                    fontWeight = FontWeight.Normal
                )

                val density = LocalDensity.current
                AnimatedVisibility(
                    visible = showTechnicalDetails,
                    enter = slideInVertically {
                        with(density) { -20.dp.roundToPx() }
                    } + expandVertically(
                        expandFrom = Alignment.Top
                    ) + fadeIn(
                        initialAlpha = 0.3f
                    ),
                    exit = slideOutVertically {
                        with(density) { 20.dp.roundToPx() }
                    } + shrinkVertically(
                        shrinkTowards = Alignment.Bottom
                    ) + fadeOut(
                        targetAlpha = 0f
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LabeledContent(
                            label = stringResource(Res.string.text_label_thumbprint),
                            content = {
                                Text(
                                    text = digest,
                                    softWrap = true,
                                )
                            },
                            modifier = contentPadding
                        )

                        LabeledContent(
                            label = stringResource(Res.string.text_label_valid_from),
                            content = {
                                Text(
                                    text = validFrom,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = contentPadding
                        )

                        LabeledContent(
                            label = stringResource(Res.string.text_label_valid_to),
                            content = {
                                Text(
                                    text = validUntil,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = contentPadding
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showTechnicalDetails = !showTechnicalDetails },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Label(
                label = if (showTechnicalDetails) {
                    stringResource(Res.string.button_label_hide_technical_details)
                } else {
                    stringResource(Res.string.button_label_show_technical_details)
                }
            )
            Icon(
                imageVector = if (showTechnicalDetails) {
                    Icons.Outlined.ArrowUpward
                } else {
                    Icons.Outlined.ArrowDownward
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    }
}