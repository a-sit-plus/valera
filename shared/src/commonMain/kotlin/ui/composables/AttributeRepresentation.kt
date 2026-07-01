package ui.composables

import androidx.compose.foundation.Image
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.dictionary_no
import at.asitplus.valera.resources.dictionary_yes
import at.asitplus.wallet.eupid.PlaceOfBirth
import at.asitplus.wallet.lib.data.LocalDateOrInstant
import at.asitplus.wallet.mdl.DrivingPrivilege
import at.asitplus.wallet.mdl.IsoSexEnum
import data.Attribute
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant


@Composable
fun AttributeRepresentation(attribute: Attribute) {
    when (attribute) {
        is Attribute.StringAttribute -> AttributeRepresentation(attribute.value)
        is Attribute.StringListAttribute -> AttributeRepresentation(attribute.value)
        is Attribute.BooleanAttribute -> AttributeRepresentation(attribute.value)
        is Attribute.DateAttribute -> AttributeRepresentation(attribute.value)
        is Attribute.DateTimeAttribute -> AttributeRepresentation(attribute.value)
        is Attribute.InstantAttribute -> AttributeRepresentation(attribute.value)
        is Attribute.GenderAttribute -> AttributeRepresentation(attribute.value.name)
        is Attribute.SexAttribute -> AttributeRepresentation(attribute.value.name)
        is Attribute.ImageAttribute -> AttributeRepresentation(attribute.value)
        is Attribute.IntegerAttribute -> AttributeRepresentation(attribute.value.toString())
        is Attribute.LongAttribute -> AttributeRepresentation(attribute.value.toString())
        is Attribute.UnsignedIntegerAttribute -> AttributeRepresentation(attribute.value.toString())
        is Attribute.PlaceOfBirthAttribute -> AttributeRepresentation(attribute.value)
        is Attribute.DrivingPrivilegeAttribute -> AttributeRepresentation(attribute.value)
    }
}


@Composable
fun AttributeRepresentation(
    value: String,
    modifier: Modifier = Modifier,
) {
    Text(
        value,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
fun AttributeRepresentation(
    value: Collection<String>,
    modifier: Modifier = Modifier,
) {
    Text(
        text = value.joinToString(", "),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}


@Composable
fun AttributeRepresentation(
    value: IsoSexEnum,
    modifier: Modifier = Modifier,
) {
    Text(
        value.name,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
fun AttributeRepresentation(
    value: Array<DrivingPrivilege>,
    modifier: Modifier = Modifier,
) {
    value.forEach {
        Text(
            text = "${it.vehicleCategoryCode} (${it.issueDate} – ${it.expiryDate}," +
                    " ${it.codes?.joinToString(separator = ", ")})",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
    }
}

@Composable
fun AttributeRepresentation(
    value: PlaceOfBirth,
    modifier: Modifier = Modifier,
) {
    Text(
        text = listOfNotNull(value.country, value.region, value.locality).joinToString(", "),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
fun AttributeRepresentation(
    value: UInt,
    modifier: Modifier = Modifier,
) {
    Text(
        value.toString(),
        modifier = modifier,
    )
}

@Composable
fun AttributeRepresentation(
    value: LocalDate,
    modifier: Modifier = Modifier,
) {
    Text(
        value.run { "$day.${month.number}.$year" },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
fun AttributeRepresentation(
    value: LocalDateTime,
    modifier: Modifier = Modifier,
) {
    Text(
        value.run { "$day.${month.number}.$year" },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
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
    value: LocalDateOrInstant,
    modifier: Modifier = Modifier,
) {
    AttributeRepresentation(
        when (value) {
            is LocalDateOrInstant.LocalDate -> value.value
            is LocalDateOrInstant.Instant -> value.value.toLocalDateTime(TimeZone.currentSystemDefault()).date
        },
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
