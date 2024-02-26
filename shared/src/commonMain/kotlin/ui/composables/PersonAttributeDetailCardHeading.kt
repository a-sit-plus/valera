package ui.composables

import AvatarHeading
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PersonAttributeDetailCardHeading(
    avatarText: String,
    title: String,
    content: (@Composable () -> Unit) = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.padding(top = 12.dp, end = 4.dp, bottom = 12.dp, start = 16.dp)
            .fillMaxWidth(),
    ) {
        AvatarHeading(
            avatarText = avatarText,
            title = title,
        )
        content()
    }
}