package at.asitplus.wallet.app.common.relyingParty.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.info_text_registration_cert_missing
import at.asitplus.valera.resources.label_registration_cert_details_country
import at.asitplus.valera.resources.label_registration_cert_details_purpose
import at.asitplus.valera.resources.label_registration_cert_details_trade_name
import at.asitplus.valera.resources.label_registration_cert_info_uri
import at.asitplus.valera.resources.label_registration_cert_more_details
import at.asitplus.valera.resources.label_registration_cert_support_uri
import at.asitplus.valera.resources.label_registration_cert_unverified
import at.asitplus.valera.resources.validation_finding_unsigned
import at.asitplus.wallet.app.common.relyingParty.RelyingPartyValidationUi
import at.asitplus.wallet.app.common.relyingParty.WrpDisplayInfo
import at.asitplus.wallet.app.common.relyingParty.getCurrentLocalization
import org.jetbrains.compose.resources.stringResource
import ui.composables.ExpandableCard
import ui.composables.LabeledHyperlinkText
import ui.composables.LabeledText
import ui.composables.validation.ValidationBanner
import ui.composables.validation.ValidationDetails

/**
 * The validation of the relying party of a request: the verdict, what the relying party is registered for, and every
 * check of its request, access certificate, and registration certificates.
 */
@Composable
fun RelyingPartyValidationView(validation: RelyingPartyValidationUi) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        validation.outcome?.let { ValidationBanner(it) }
        if (validation.unsigned) {
            Text(stringResource(Res.string.validation_finding_unsigned))
        }
        if (validation.registrationMissing) {
            Text(stringResource(Res.string.info_text_registration_cert_missing), fontWeight = FontWeight.Bold)
        }
        validation.registration?.let { WrprcDetailsCard(it, verified = validation.registrationVerified) }
        validation.outcome?.let { ValidationDetails(it) }
    }
}

/** What a registration certificate says, marked if it has not been [verified]. */
@Composable
fun WrprcDetailsCard(displayInfo: WrpDisplayInfo, verified: Boolean) {
    ExpandableCard(
        text = stringResource(
            if (verified) Res.string.label_registration_cert_more_details
            else Res.string.label_registration_cert_unverified
        ),
        icon = Icons.Outlined.Info,
        expanded = false
    ) {
        Column(modifier = Modifier.padding(start = 20.dp, top = 16.dp)) {
            val paddingModifier = Modifier.padding(bottom = 16.dp)
            displayInfo.name?.let { name ->
                LabeledText(
                    label = stringResource(Res.string.label_registration_cert_details_trade_name),
                    text = name,
                    modifier = paddingModifier,
                )
            }
            displayInfo.purpose?.getCurrentLocalization()?.let { name ->
                LabeledText(
                    label = stringResource(Res.string.label_registration_cert_details_purpose),
                    text = name,
                    modifier = paddingModifier,
                    maxLines = 2
                )
            }
            displayInfo.country?.let { country ->
                LabeledText(
                    label = stringResource(Res.string.label_registration_cert_details_country),
                    text = country,
                    modifier = paddingModifier,
                    maxLines = 2
                )
            }
            displayInfo.infoUri?.let { infoUri ->
                LabeledHyperlinkText(
                    label = stringResource(Res.string.label_registration_cert_info_uri),
                    text = infoUri,
                    url = infoUri,
                    modifier = paddingModifier,
                )
            }
            displayInfo.supportUri?.let { supportUri ->
                LabeledHyperlinkText(
                    label = stringResource(Res.string.label_registration_cert_support_uri),
                    text = supportUri,
                    url = supportUri,
                    modifier = paddingModifier,
                )
            }
        }
    }
}
