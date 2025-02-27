package ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.attribute_friendly_name_data_recipient_name
import at.asitplus.valera.resources.text_label_optional
import org.jetbrains.compose.resources.stringResource

@Composable
fun ConsentAttributesSection(
    title: String,
    attributes: Map<String, Boolean>,
    modifier: Modifier = Modifier,
) {
    val optionalText = stringResource(Res.string.text_label_optional)

    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column(modifier = Modifier.padding(start = 32.dp)) {
            attributes.forEach {
                Text(
                    when (it.value) {
                        true -> "${it.key} ($optionalText)"
                        false -> it.key
                    }
                )
            }
        }
    }
}