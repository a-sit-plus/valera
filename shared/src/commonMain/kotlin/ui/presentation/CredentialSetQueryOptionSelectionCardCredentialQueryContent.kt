package ui.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
) {
    if(credentialRepresentationLocalized != null) {
        LabeledText(
            text = credentialSchemeLocalized,
            label = credentialRepresentationLocalized,
        )
    } else {
        Text(
            text = credentialSchemeLocalized,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    credentialAttributesLocalized?.let { (attributeNames, otherClaimReferences) ->
        Column(
            modifier = Modifier.padding(start = 8.dp)
        ) {
            attributeNames.forEachIndexed { index, it ->
                Text(it)
            }
            if(otherClaimReferences > 0) {
                Text("+$otherClaimReferences " + stringResource(Res.string.additional_text_other_untranslated_claims))
            }
        }
    } ?: Text(stringResource(Res.string.text_label_all_claims_requested)) // all
}