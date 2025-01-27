package ui.composables.credentials

import androidx.compose.runtime.Composable
import data.PersonalDataCategory
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ui.composables.buttons.ExpandButtonUpDown
import ui.composables.PersonAttributeDetailCardHeading

@Composable
fun CredentialDetailCardHeader(
    isExpanded: Boolean,
    onChangeIsExpanded: (Boolean) -> Unit,
    dataCategory: PersonalDataCategory,
) = dataCategory.run {
    CredentialDetailCardHeader(
        iconText = stringResource(iconText),
        title = stringResource(categoryTitle),
        isExpanded = isExpanded,
        onChangeIsExpanded = onChangeIsExpanded,
    )
}

@Composable
fun CredentialDetailCardHeader(
    isExpanded: Boolean,
    onChangeIsExpanded: (Boolean) -> Unit,
    iconText: StringResource,
    title: StringResource,
) {
    CredentialDetailCardHeader(
        iconText = stringResource(iconText),
        title = stringResource(title),
        isExpanded = isExpanded,
        onChangeIsExpanded = onChangeIsExpanded,
    )
}

@Composable
fun CredentialDetailCardHeader(
    isExpanded: Boolean,
    onChangeIsExpanded: (Boolean) -> Unit,
    iconText: String,
    title: String,
) {
    PersonAttributeDetailCardHeading(
        iconText = iconText,
        title = title
    ) {
        ExpandButtonUpDown(
            isExpanded = isExpanded,
            onClick = {
                onChangeIsExpanded(!isExpanded)
            },
            contentDescription = null
        )
    }
}
