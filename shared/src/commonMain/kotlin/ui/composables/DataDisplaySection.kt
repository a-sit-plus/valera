package ui.composables

import LabeledText
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DataDisplaySection(
    title: String,
    data: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.SemiBold,
        )
        Column(modifier = Modifier.padding(start = 32.dp)) {
            val paddingModifier = Modifier.padding(bottom = 16.dp)
            for (labeledItem in data) {
                LabeledText(
                    label = labeledItem.first,
                    text = labeledItem.second,
                    modifier = paddingModifier,
                )
            }
        }
    }
}