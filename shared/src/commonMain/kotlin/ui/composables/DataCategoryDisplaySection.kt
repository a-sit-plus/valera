package ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Badge
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import composewalletapp.shared.generated.resources.INFO_TEXT_DATA_ITEMS_MISSING
import composewalletapp.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource


data class AttributeAvailability(
    val attributeName: String,
    val isAvailable: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun DataCategoryDisplaySection(
    title: String,
    attributes: List<Pair<PersonalDataCategory, List<AttributeAvailability>>>,
    modifier: Modifier = Modifier,
) {
    val openSections = rememberSaveable {
        mutableStateOf<Set<Int>>(
            setOf()
        )
    }

    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.SemiBold,
        )
        Column {
            val paddingModifier =
                Modifier.padding(top = 8.dp, end = 24.dp, bottom = 8.dp, start = 16.dp)
            for (categoryIndex in attributes.indices) {
                val category = attributes[categoryIndex]
                val missingAttributeCount = category.second.count {
                    !it.isAvailable
                }
                Column {
                    Row(
                        modifier = paddingModifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        DataCategoryDisplaySectionItem(
                            iconText = stringResource(category.first.iconText),
                            iconColor = MaterialTheme.colorScheme.secondaryContainer,
                            title = stringResource(category.first.categoryTitle),
                            titleFontWeight = FontWeight.Normal,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxHeight(),
                            ) {
                                if (missingAttributeCount > 0) {
                                    Text(
                                        text = stringResource(Res.string.INFO_TEXT_DATA_ITEMS_MISSING),
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                    ) {
                                        Text(
                                            text = missingAttributeCount.toString(),
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        if (openSections.value.contains(categoryIndex)) {
                                            openSections.value = openSections.value - categoryIndex
                                        } else {
                                            openSections.value = openSections.value + categoryIndex
                                        }
                                    },
                                ) {
                                    if (openSections.value.contains(categoryIndex)) {
                                        Icon(
                                            imageVector = Icons.Default.ExpandLess,
                                            contentDescription = null
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.ExpandMore,
                                            contentDescription = null
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (openSections.value.contains(categoryIndex)) {
                        Divider(modifier = Modifier.fillMaxWidth())
                        for (item in category.second) {
                            DataCategoryDisplaySectionItem(
                                iconText = "",
                                title = item.attributeName,
                                titleFontWeight = FontWeight.Normal,
                                iconColor = Color.Unspecified,
                                iconContentColor = Color.Unspecified,
                                titleColor = if (item.isAvailable == false) MaterialTheme.colorScheme.error else Color.Unspecified,
                                modifier = paddingModifier.fillMaxWidth(),
                            )
                        }
                        Divider(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
fun DataCategoryDisplaySectionItem(
    title: String,
    titleColor: Color = Color.Unspecified,
    titleFontWeight: FontWeight = FontWeight.SemiBold,
    iconText: String,
    iconColor: Color = TextIconDefaults.color(),
    iconContentColor: Color = contentColorFor(iconColor),
    iconTextFontWeight: FontWeight = FontWeight.Bold,
    modifier: Modifier = Modifier,
    actionButtons: (@Composable RowScope.() -> Unit) = { },
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeadingTextIcon(
                text = iconText,
                color = iconColor,
                contentColor = iconContentColor,
                fontWeight = iconTextFontWeight,
            )

            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor,
                fontWeight = titleFontWeight,
            )
        }
        actionButtons()
    }
}