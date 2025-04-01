package ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import data.Attribute


@Composable
fun LabeledAttribute(
    label: String,
    attribute: Attribute,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
    ) {
        AttributeRepresentation(attribute)
        Label(label)
    }
}