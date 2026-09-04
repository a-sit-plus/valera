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

/**
 * One row of a consent list. Renders the value where the credential has a displayable one, and the label on its own
 * otherwise, so an attribute that is about to be disclosed is never omitted just because it cannot be rendered.
 */
@Composable
fun DisclosedAttribute(
    label: String,
    attribute: Attribute?,
    modifier: Modifier = Modifier,
) {
    if (attribute != null) {
        LabeledAttribute(label = label, attribute = attribute, modifier = modifier)
    } else {
        Column(modifier = modifier) {
            Label(label)
        }
    }
}
