
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ui.composables.TextAvatar
import ui.composables.TextAvatarDefaults

@Composable
fun AvatarHeading(
    avatarText: String?,
    title: String,
    avatarColors: IconButtonColors = TextAvatarDefaults.backgroundColor(),
    textColor: Color = Color.Unspecified,
    fontWeight: FontWeight = FontWeight.SemiBold,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        TextAvatar(
            text = avatarText,
            enabled = enabled,
            colors = avatarColors,
            onClick = onClick,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = textColor,
            fontWeight = fontWeight,
        )
    }
}