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
    val firstname: String?,
    val lastname: String?,
    val dateOfBirth: LocalDate?,
    val portrait: ImageBitmap?,
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
            if (identityData.portrait != null) {
                Image(
                    bitmap = identityData.portrait,
                    contentDescription = "Picture"
                )
            }
            Column {
                Text("${identityData.firstname} ${identityData.lastname}")
                if(identityData.dateOfBirth != null) {
                    Text(identityData.dateOfBirth.run { "$dayOfMonth.$monthNumber.$year" })
                }
            }
        }
    }
}