package ui.composables

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonChipDataDetailCard(
    personalDataCategory: PersonalDataCategory,
    chipStrings: List<String>,
    actionButton: (@Composable () -> Unit)? = null,
    onDetailClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    PersonAttributeDetailCard(
        personalDataCategory = personalDataCategory,
        actionButton = actionButton,
        onDetailClick = onDetailClick,
        modifier = modifier,
    ) {
        FlowRow {
            for (chipString in chipStrings) {
                SuggestionChip(
                    label = {
                        Text(text = chipString)
                    },
                    onClick = {},
                )
            }
        }
    }
}