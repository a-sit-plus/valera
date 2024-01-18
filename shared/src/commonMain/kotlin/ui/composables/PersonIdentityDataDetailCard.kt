package ui.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.datetime.LocalDate

data class IdentityData(
    val name: String,
    val birthdate: LocalDate,
    val picture: ImageBitmap?,
)

@Composable
fun PersonIdentityDataDetailCard(
    identityData: IdentityData,
    onDetailClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    PersonAttributeDetailCard(
        personalDataCategory = PersonalDataCategory.IdentityData,
        onDetailClick = onDetailClick,
        modifier = modifier,
    ) {
        Row {
            if (identityData.picture != null) {
                Image(
                    bitmap = identityData.picture,
                    contentDescription = "Picture"
                )
            }
            Column {
                Text(identityData.name)
                Text(identityData.birthdate.run { "$dayOfMonth.$monthNumber.$year" })
            }
        }
    }
}