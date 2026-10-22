package ui.composables.validation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.label_issuer_registration
import at.asitplus.valera.resources.label_issuer_registration_attestations
import at.asitplus.valera.resources.label_issuer_registration_description
import at.asitplus.valera.resources.label_issuer_registration_identifiers
import at.asitplus.valera.resources.label_issuer_registration_registry
import at.asitplus.valera.resources.label_issuer_registration_unverified
import at.asitplus.valera.resources.label_registration_cert_details_trade_name
import at.asitplus.valera.resources.section_heading_issuer_validation
import at.asitplus.valera.resources.validation_finding_unsigned
import at.asitplus.wallet.app.common.relyingParty.getCurrentLocalization
import at.asitplus.wallet.app.common.validation.IssuerRegistrationUi
import at.asitplus.wallet.app.common.validation.IssuerValidationUi
import org.jetbrains.compose.resources.stringResource
import ui.composables.ExpandableCard
import ui.composables.LabeledHyperlinkText
import ui.composables.LabeledText

/**
 * The validation of a credential issuer before issuance: the verdict on its metadata, its registration, and every
 * check of its metadata and registration certificate. Whether it is registered for an offered credential is shown with
 * that credential, see `CredentialIdentifierSelection`.
 */
@Composable
fun IssuerValidationView(issuer: IssuerValidationUi, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.section_heading_issuer_validation),
            style = MaterialTheme.typography.titleMedium,
        )
        issuer.outcome?.let { ValidationBanner(it) }
            ?: Text(stringResource(Res.string.validation_finding_unsigned))
        issuer.registration?.let { IssuerRegistrationCard(it, verified = issuer.verified) }
        issuer.outcome?.let { ValidationDetails(it) }
    }
}

/** What a registrar says about a credential issuer, marked if it has not been [verified]. */
@Composable
private fun IssuerRegistrationCard(registration: IssuerRegistrationUi, verified: Boolean) {
    ExpandableCard(
        text = stringResource(
            if (verified) Res.string.label_issuer_registration else Res.string.label_issuer_registration_unverified
        ),
        icon = Icons.Outlined.Info,
        expanded = false,
    ) {
        Column(modifier = Modifier.padding(start = 20.dp, top = 16.dp)) {
            val paddingModifier = Modifier.padding(bottom = 16.dp)
            registration.name?.let {
                LabeledText(
                    label = stringResource(Res.string.label_registration_cert_details_trade_name),
                    text = it,
                    modifier = paddingModifier,
                )
            }
            registration.description.getCurrentLocalization()?.let {
                LabeledText(
                    label = stringResource(Res.string.label_issuer_registration_description),
                    text = it,
                    modifier = paddingModifier,
                    maxLines = 3,
                )
            }
            if (registration.identifiers.isNotEmpty()) {
                LabeledText(
                    label = stringResource(Res.string.label_issuer_registration_identifiers),
                    text = registration.identifiers.joinToString("\n"),
                    modifier = paddingModifier,
                    maxLines = registration.identifiers.size,
                )
            }
            if (registration.attestations.isNotEmpty()) {
                LabeledText(
                    label = stringResource(Res.string.label_issuer_registration_attestations),
                    text = registration.attestations.joinToString("\n"),
                    modifier = paddingModifier,
                    maxLines = registration.attestations.size,
                )
            }
            LabeledHyperlinkText(
                label = stringResource(Res.string.label_issuer_registration_registry),
                text = registration.registryUri,
                url = registration.registryUri,
                modifier = paddingModifier,
            )
        }
    }
}
