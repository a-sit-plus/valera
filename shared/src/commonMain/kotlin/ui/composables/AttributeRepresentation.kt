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
import at.asitplus.wallet.companyregistration.Address
import at.asitplus.wallet.companyregistration.Branch
import at.asitplus.wallet.companyregistration.CompanyActivity
import at.asitplus.wallet.companyregistration.ContactData
import at.asitplus.wallet.mdl.DrivingPrivilege
import at.asitplus.wallet.mdl.IsoSexEnum
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
        // TODO Nice representation for driving privileges
        is Attribute.DrivingPrivilegeAttribute -> AttributeRepresentation(attribute.value)
        is Attribute.AddressAttribute -> AttributeRepresentation(attribute.value)
        is Attribute.BranchAttribute -> AttributeRepresentation(attribute.value)
        is Attribute.CompanyActivityAttribute -> AttributeRepresentation(attribute.value)
        is Attribute.ContactDataAttribute -> AttributeRepresentation(attribute.value)
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
    value.forEach { privilege ->
        Text(
            text = "${privilege.vehicleCategoryCode} (${privilege.issueDate} | ${privilege.expiryDate})",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
    }
}

@Composable
fun AttributeRepresentation(
    value: CompanyActivity,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "${value.naceCode} (${value.description})",
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
fun AttributeRepresentation(
    value: ContactData,
    modifier: Modifier = Modifier,
) {
    Text(
        text = listOfNotNull(value.email, value.telephone).joinToString(", "),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
fun AttributeRepresentation(
    value: Address,
    modifier: Modifier = Modifier,
) {
    Text(
        text = listOfNotNull(
            value.thoroughfare,
            value.locatorDesignator,
            value.poBox,
            value.postCode,
            value.postName,
            value.adminUnitLevel2,
            value.adminUnitLevel1
        ).joinToString(", "),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
fun AttributeRepresentation(
    value: Branch,
    modifier: Modifier = Modifier,
) {
    Text(
        text = listOfNotNull(
            value.name,
            value.euid,
        ).joinToString(", "),
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
        value.run { "$dayOfMonth.$monthNumber.$year" },
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
        value.run { "$dayOfMonth.$monthNumber.$year" },
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
