package ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class AgeData(
    val ageLowerBounds: List<Int> = listOf(),
    val ageUpperBounds: List<Int> = listOf(),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonAgeDataDetailCard(
    ageData: AgeData,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            PersonAttributeDetailCardHeading(
                avatarText = "AS",
                title = "Altersstufen",
            )
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
            ) {
                for (ageString in ageData.ageLowerBounds.sorted().map { "≥$it" }) {
                    SuggestionChip(
                        label = {
                            Text(text = ageString)
                        },
                        onClick = {},
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
            ) {
                for (ageString in ageData.ageUpperBounds.sorted().map { "<$it" }) {
                    SuggestionChip(
                        label = {
                            Text(text = ageString)
                        },
                        onClick = {},
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}