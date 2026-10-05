package ui.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import at.asitplus.iso.AgeAttestation
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.additional_text_other_untranslated_claims
import at.asitplus.valera.resources.text_label_age_attestation_substitution
import at.asitplus.valera.resources.text_label_all_claims_requested
import org.jetbrains.compose.resources.stringResource
import ui.composables.LabeledText

@Composable
fun ColumnScope.CredentialSetQueryOptionSelectionCardCredentialQueryContent(
    credentialSchemeLocalized: String,
    credentialAttributesLocalized: Pair<List<String>, Int>?,
    credentialRepresentationLocalized: String? = null,
    credentialAllowedAttributes:  Map<String, Boolean>? = null,
) {
    if (credentialRepresentationLocalized != null) {
        LabeledText(
            text = credentialSchemeLocalized,
            label = credentialRepresentationLocalized,
        )
    } else {
        BoldCredentialSchemeText(credentialSchemeLocalized)
    }
    credentialAttributesLocalized?.let { (attributeNames, otherClaimReferences) ->
        Column(
            modifier = Modifier.padding(start = 8.dp)
        ) {
            attributeNames.forEachIndexed { index, it ->
                val allowed = credentialAllowedAttributes?.get(it) ?: true
                val color = when(allowed) {
                    false -> Color.Red
                    else -> Color.Unspecified
                }
                Text(text = it, color = color)
            }
            if (otherClaimReferences > 0) {
                Text("+$otherClaimReferences " + stringResource(Res.string.additional_text_other_untranslated_claims))
            }
            // An age attestation the issuer did not provision has no type-metadata label, so it reaches this list
            // as the raw `age_over_NN` identifier. That is also exactly the case where ISO/IEC 18013-5, 7.2.5 makes
            // the wallet answer with the nearest attestation the credential does carry, so the element finally
            // disclosed can name a different age than the one requested here.
            if (attributeNames.any { AgeAttestation.isAgeAttestation(it) }) {
                Text(
                    text = stringResource(Res.string.text_label_age_attestation_substitution),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } ?: Text(stringResource(Res.string.text_label_all_claims_requested)) // all
}