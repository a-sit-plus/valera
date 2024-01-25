import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable

@Composable
fun ExpandButton(
    isExpanded: Boolean,
    onClick: () -> Unit,
    contentDescription: String?,
) {
    IconButton(
        onClick = onClick,
    ) {
        Icon(
            imageVector = if(isExpanded) {
                Icons.Default.ExpandLess
            } else {
                Icons.Default.ExpandMore
            },
            contentDescription = contentDescription
        )
    }
}