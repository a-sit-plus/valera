package ui.composables

import androidx.compose.foundation.Image
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.dictionary_no
import composewalletapp.shared.generated.resources.dictionary_yes
import data.Attribute
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource


@Composable
fun AttributeRepresentation(attribute: Attribute) {
    when (attribute) {
        is Attribute.StringAttribute -> AttributeRepresentation(attribute.value)
        is Attribute.BooleanAttribute -> AttributeRepresentation(attribute.value)
        is Attribute.DateAttribute -> AttributeRepresentation(attribute.value)
        is Attribute.GenderAttribute -> TODO() // AttributeRepresentation(attribute.value)
        is Attribute.ImageAttribute -> AttributeRepresentation(attribute.value)
        is Attribute.IntegerAttribute -> TODO() // AttributeRepresentation(attribute.value)
        is Attribute.UnsignedIntegerAttribute -> TODO() // AttributeRepresentation(attribute.value)
        is Attribute.DrivingPrivilegeAttribute -> TODO() // AttributeRepresentation(attribute.value)
    }
}


@Composable
fun AttributeRepresentation(value: String) {
    Text(value)
}


@Composable
fun AttributeRepresentation(value: LocalDate) {
    Text(value.run { "$dayOfMonth.$monthNumber.$year" })
}


@Composable
fun AttributeRepresentation(
    value: ImageBitmap,
) {
    Image(
        bitmap = value,
        contentDescription = null,
    )
}


@Composable
fun AttributeRepresentation(
    value: Boolean,
) {
    AttributeRepresentation(
        if (value) {
            stringResource(Res.string.dictionary_yes)
        } else {
            stringResource(Res.string.dictionary_no)
        }
    )
}