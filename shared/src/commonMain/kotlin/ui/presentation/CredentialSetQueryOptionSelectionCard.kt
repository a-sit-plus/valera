package ui.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.text_label_selected_for_presentation
import org.jetbrains.compose.resources.stringResource

@Composable
fun CredentialSetQueryOptionSelectionCard(
    credentialRepresentationLocalized: String?,
    credentialSchemeLocalized: String,
    credentialAttributesLocalized: Pair<List<String>, Int>?,
    colors: CardColors? = null,
    isSelectedForPresentation: Boolean = false,
    isFaded: Boolean = false,
    credentialAllowedAttributes:  Map<String, Boolean>? = null,
) {
// No credentials available, show the query that didn't match against anything
    ElevatedCard(
        colors = colors ?: if (isSelectedForPresentation) {
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        } else {
            CardDefaults.elevatedCardColors()
        },
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isFaded) 0.45f else 1f)
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            if (isSelectedForPresentation) {
                Text(
                    text = stringResource(Res.string.text_label_selected_for_presentation),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            CredentialSetQueryOptionSelectionCardCredentialQueryContent(
                credentialRepresentationLocalized = credentialRepresentationLocalized,
                credentialSchemeLocalized = credentialSchemeLocalized,
                credentialAttributesLocalized = credentialAttributesLocalized,
                credentialAllowedAttributes = credentialAllowedAttributes
            )
        }
    }
}
