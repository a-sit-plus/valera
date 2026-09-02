package ui.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.additional_text_other_untranslated_claims
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
        }
    } ?: Text(stringResource(Res.string.text_label_all_claims_requested)) // all
}