package ui.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.dictionary_yes
import composewalletapp.shared.generated.resources.dictionary_no
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

@Composable
fun AttributeRepresentation(value: String) {
    Text(value)
}


@Composable
fun AttributeRepresentation(value: LocalDate) {
    Text(value.run { "$dayOfMonth.$monthNumber.$year" })
}



@Composable
fun AttributeRepresentation(
    value: ImageBitmap,
    contentDescription: String? = null,
) {
    Image(
        bitmap = value,
        contentDescription = contentDescription,
    )
}




@Composable
fun AttributeRepresentation(
    value: Boolean,
) {
    AttributeRepresentation(
        if(value) {
            stringResource(Res.string.dictionary_yes)
        } else {
            stringResource(Res.string.dictionary_no)
        }
    )
}
