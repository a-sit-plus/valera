package ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import at.asitplus.signum.indispensable.asn1.encoding.decodeToString
import at.asitplus.signum.indispensable.pki.AttributeTypeAndValue
import at.asitplus.signum.indispensable.pki.X509Certificate
import at.asitplus.signum.indispensable.requireSupported
import at.asitplus.signum.supreme.hash.digest
import at.asitplus.valera.resources.Res
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
import at.asitplus.wallet.app.common.digest
import org.jetbrains.compose.resources.stringResource

@Composable
fun IssuerTrustDetailsCard(
    certificate: X509Certificate,
    trustState: TrustState,
    modifier: Modifier = Modifier
) {
    val contentPadding = Modifier.padding(bottom = 16.dp, end = 16.dp, start = 16.dp)

    val rdnAttributes = certificate.tbsCertificate.subjectName.flatMap { it.attrsAndValues }
    val issuerName = rdnAttributes.firstOrNull { it is AttributeTypeAndValue.Organization }?.value?.asPrimitive()?.decodeToString()
        ?: rdnAttributes.firstOrNull { it is AttributeTypeAndValue.CommonName }?.value?.asPrimitive()?.decodeToString()
        ?: "Unknown Issuer"

    val validFrom = certificate.tbsCertificate.validFrom.instant.toString()
    val validUntil = certificate.tbsCertificate.validUntil.instant.toString()
    val digest = certificate.signatureAlgorithm.let {
        it.requireSupported()
        it.digest.digest(certificate.encodeToDer())
            .toHexString(HexFormat.UpperCase)
            .chunked(2)
            .joinToString(":")
    }
    val displayTrust = when (trustState) {
        TrustState.TRUSTED -> stringResource(Res.string.trust_status_trusted)
        TrustState.UNTRUSTED -> stringResource(Res.string.trust_status_untrusted)
        TrustState.UNKNOWN -> stringResource(Res.string.trust_status_unknown)
        TrustState.EVALUATING -> stringResource(Res.string.trust_status_evaluating)
    }

    ElevatedCard(modifier = modifier.padding(bottom = 16.dp)) {
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
