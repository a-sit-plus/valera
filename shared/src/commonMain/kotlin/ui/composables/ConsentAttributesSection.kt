package ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.text_label_all_attributes
import at.asitplus.valera.resources.text_label_optional
import org.jetbrains.compose.resources.stringResource

@Composable
fun ConsentAttributesSection(
    title: String,
    attributes: Pair<Int, Map<String, Boolean>>?, //<key = attribute name, value = optional>
    modifier: Modifier = Modifier,
) {
    val optionalText = stringResource(Res.string.text_label_optional)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.SemiBold,
        )
        Column(modifier = Modifier.padding(start = 32.dp)) {
            attributes?.let { (otherClaims, attributes) ->
                attributes.forEach {
                    Text(
                        when (it.value) {
                            true -> "${it.key} ($optionalText)"
                            false -> it.key
                        }
                    )
                }
                // TODO: do we want to show how many non-single claim queries exist?
            } ?: run {
                Text(stringResource(Res.string.text_label_all_attributes))
            }
        }
    }
}