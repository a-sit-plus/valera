package data.bletransfer.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

class Entry(
    private val displayName: StringResource,
    private val value: EntryValue?
) {
    @Composable
    fun show() {
        SectionTitle(stringResource(displayName))
        Box(modifier = Modifier.padding(8.dp)) {
            value?.showData()
        }
    }
}
