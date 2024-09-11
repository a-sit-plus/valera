package ui.composables

import androidx.compose.foundation.Image
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.dictionary_no
import compose_wallet_app.shared.generated.resources.dictionary_yes
import data.Attribute
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource


@Composable
fun AttributeRepresentation(attribute: Attribute) {
    when (attribute) {
        is Attribute.StringAttribute -> AttributeRepresentation(attribute.value)
        is Attribute.BooleanAttribute -> AttributeRepresentation(attribute.value)
        is Attribute.DateAttribute -> AttributeRepresentation(attribute.value)
        is Attribute.DateTimeAttribute -> AttributeRepresentation(attribute.value)
        is Attribute.InstantAttribute -> AttributeRepresentation(attribute.value)
        is Attribute.GenderAttribute -> TODO() // AttributeRepresentation(attribute.value)
        is Attribute.ImageAttribute -> AttributeRepresentation(attribute.value)
        is Attribute.IntegerAttribute -> TODO() // AttributeRepresentation(attribute.value)
        is Attribute.UnsignedIntegerAttribute -> TODO() // AttributeRepresentation(attribute.value)
        is Attribute.DrivingPrivilegeAttribute -> TODO() // AttributeRepresentation(attribute.value)
    }
}


@Composable
fun AttributeRepresentation(
    value: String,
    modifier: Modifier = Modifier,
) {
    Text(
        value,
        modifier = modifier,
    )
}


@Composable
fun AttributeRepresentation(
    value: LocalDate,
    modifier: Modifier = Modifier,
) {
    Text(
        value.run { "$dayOfMonth.$monthNumber.$year" },
        modifier = modifier,
    )
}

@Composable
fun AttributeRepresentation(
    value: LocalDateTime,
    modifier: Modifier = Modifier,
) {
    Text(
        value.run { "$dayOfMonth.$monthNumber.$year" },
        modifier = modifier,
    )
}

@Composable
fun AttributeRepresentation(
    value: Instant,
    modifier: Modifier = Modifier,
) {
    AttributeRepresentation(
        value.toLocalDateTime(TimeZone.currentSystemDefault()),
        modifier = modifier
    )
}


@Composable
fun AttributeRepresentation(
    value: ImageBitmap,
    modifier: Modifier = Modifier,
) {
    Image(
        bitmap = value,
        contentDescription = null,
        modifier = modifier,
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