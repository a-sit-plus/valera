package ui.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.asp
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun Logo(onClick: () -> Unit) {
    Image(
        modifier = Modifier
            .padding(start = 0.dp, end = 8.dp, top = 0.dp)
            .size(65.9.dp, 19.7.dp)
            .clickable(onClick = onClick),
        painter = painterResource(Res.drawable.asp),
        contentDescription = null,
        contentScale = ContentScale.Inside,
    )
}