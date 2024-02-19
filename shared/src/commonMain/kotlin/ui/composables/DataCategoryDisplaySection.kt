package ui.composables

import AvatarHeading
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


data class AttributeAvailability(
    val attributeName: String,
    val isAvailable: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
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
                Modifier.padding(top = 4.dp, end = 24.dp, start = 16.dp, bottom = 4.dp)
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
                        AvatarHeading(
                            avatarText = category.first.avatarText,
                            title = category.first.categoryName,
                            fontWeight = FontWeight.Normal,
                        )
                        Row(
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (missingAttributeCount > 0) {
                                Text(
                                    text = "fehlend ",
                                    color = MaterialTheme.colorScheme.error,
                                )
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
                    if (openSections.value.contains(categoryIndex)) {
                        Divider(modifier = Modifier.fillMaxWidth())
                        for (item in category.second) {
                            Row(
                                modifier = paddingModifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                AvatarHeading(
                                    avatarText = null,
                                    title = item.attributeName,
                                    fontWeight = FontWeight.Normal,
                                    color = if (item.isAvailable == false) MaterialTheme.colorScheme.error else Color.Unspecified,
                                    enabled = false,
                                )
                            }
                        }
                        Divider(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}