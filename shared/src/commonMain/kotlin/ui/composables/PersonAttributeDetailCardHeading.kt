package ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PersonAttributeDetailCardHeading(
    title: String,
    iconText: String,
    actionButtons: (@Composable RowScope.() -> Unit) = {},
) {
    PersonAttributeDetailCardHeading(
        icon = {
            PersonAttributeDetailCardHeadingIcon(iconText)
        },
        title = {
            PersonAttributeDetailCardHeadingText(title)
        },
    ) {
        actionButtons()
    }
}

@Composable
fun PersonAttributeDetailCardHeading(
    icon: @Composable RowScope.() -> Unit,
    title: @Composable RowScope.() -> Unit,
    actionButtons: (@Composable RowScope.() -> Unit) = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.padding(top = 12.dp, end = 4.dp, bottom = 12.dp, start = 16.dp)
            .fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Spacer(modifier = Modifier.width(16.dp))
            title()
        }
        actionButtons()
    }
}

@Composable
fun PersonAttributeDetailCardHeadingText(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier,
    )
}

@Composable
fun PersonAttributeDetailCardHeadingIcon(
    iconText: String,
    modifier: Modifier = Modifier,
) {
    HeadingTextIcon(
        text = iconText,
        fontWeight = FontWeight.Bold,
        modifier = modifier,
    )
}

