package ui.composables.credentials

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.composables.LabeledContent

@Composable
fun GenericDataCardContent(
    items: List<Pair<String, @Composable () -> Unit>>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        items.mapIndexed { index, it ->
            LabeledContent(
                label = it.first,
                content = it.second,
                modifier = if (index == items.lastIndex) {
                    Modifier
                } else {
                    Modifier.padding(bottom = 8.dp)
                }
            )
        }
    }
}
