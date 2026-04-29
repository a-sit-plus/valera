package ui.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CredentialSetQueryOptionSelectionCard(
    credentialRepresentationLocalized: String?,
    credentialSchemeLocalized: String,
    credentialAttributesLocalized: Pair<List<String>, Int>?,
    colors: CardColors? = null
) {
// No credentials available, show the query that didn't match against anything
    ElevatedCard(
        colors = colors ?: CardDefaults.elevatedCardColors(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            CredentialSetQueryOptionSelectionCardCredentialQueryContent(
                credentialRepresentationLocalized = credentialRepresentationLocalized,
                credentialSchemeLocalized = credentialSchemeLocalized,
                credentialAttributesLocalized = credentialAttributesLocalized,
            )
        }
    }
}