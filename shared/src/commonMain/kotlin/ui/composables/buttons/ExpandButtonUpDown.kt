import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable

@Composable
fun ExpandButtonUpDown(
    isExpanded: Boolean,
    onClick: () -> Unit,
    contentDescription: String?,
) {
    IconButton(
        onClick = onClick,
    ) {
        Icon(
            imageVector = if(isExpanded) {
                Icons.Default.ArrowDropUp
            } else {
                Icons.Default.ArrowDropDown
            },
            contentDescription = contentDescription
        )
    }
}