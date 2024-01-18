package ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class AgeData(
    val ageLowerBounds: List<Int> = listOf(),
    val ageUpperBounds: List<Int> = listOf(),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonAgeDataDetailCard(
    ageData: AgeData,
    onDetailClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    PersonAttributeDetailCard(
        personalDataCategory = PersonalDataCategory.AgeData,
        onDetailClick = onDetailClick,
        modifier = modifier,
    ) {
        Column {
            FlowRow {
                for (ageString in ageData.ageLowerBounds.sorted().map { "≥$it" }) {
                    SuggestionChip(
                        label = {
                            Text(text = ageString)
                        },
                        onClick = {},
                    )
                }
            }
            FlowRow {
                for (ageString in ageData.ageUpperBounds.sorted().map { "<$it" }) {
                    SuggestionChip(
                        label = {
                            Text(text = ageString)
                        },
                        onClick = {},
                    )
                }
            }
        }
    }
}