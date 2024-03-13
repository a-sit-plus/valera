package ui.composables

import Resources
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import data.CredentialExtractor
import kotlinx.datetime.LocalDate

data class PreIdentityData(
    val firstname: String?,
    val lastname: String?,
    val dateOfBirth: LocalDate?,
    val portrait: ByteArray?,
) {
    fun toIdentityData(decodeImage: (ByteArray) -> ImageBitmap): IdentityData {
        return IdentityData(
            firstname = firstname,
            lastname = lastname,
            dateOfBirth = dateOfBirth,
            portrait = portrait?.let { decodeImage(it) }
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as PreIdentityData

        if (firstname != other.firstname) return false
        if (lastname != other.lastname) return false
        if (dateOfBirth != other.dateOfBirth) return false
        if (portrait != null) {
            if (other.portrait == null) return false
            if (!portrait.contentEquals(other.portrait)) return false
        } else if (other.portrait != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = firstname?.hashCode() ?: 0
        result = 31 * result + (lastname?.hashCode() ?: 0)
        result = 31 * result + (dateOfBirth?.hashCode() ?: 0)
        result = 31 * result + (portrait?.contentHashCode() ?: 0)
        return result
    }
}

data class IdentityData(
    val firstname: String?,
    val lastname: String?,
    val dateOfBirth: LocalDate?,
    val portrait: ImageBitmap?,
)

val CredentialExtractor.preIdentityData: PreIdentityData
    get() = PreIdentityData(
        firstname = this.givenName,
        lastname = this.familyName,
        dateOfBirth = this.dateOfBirth,
        portrait = this.portrait,
    )

@Composable
fun PersonIdentityDataDetailCard(
    identityData: IdentityData,
    onClickOpenDetails: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            PersonAttributeDetailCardHeading(
                iconText = PersonalDataCategory.IdentityData.iconText,
                title = PersonalDataCategory.IdentityData.categoryTitle,
            ) {
                if (onClickOpenDetails != null) {
                    IconButton(
                        onClick = onClickOpenDetails
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null
                        )
                    }
                }
            }

            var columnSize by remember { mutableStateOf(Size.Zero) }
            Row(
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth().onGloballyPositioned { layoutCoordinates ->
                        columnSize = layoutCoordinates.size.toSize()
                    }
            ) {
                if (identityData.portrait != null) {
                    // source: https://stackoverflow.com/questions/69455135/does-jetpack-compose-have-maxheight-and-maxwidth-like-xml
                    // weird way to get "at most 1/4th of max width"
                    val maxWidth = LocalDensity.current.run { (0.25f * columnSize.width).toDp() }
                    Image(
                        bitmap = identityData.portrait,
                        contentDescription = Resources.CONTENT_DESCRIPTION_PORTRAIT,
                        modifier = Modifier.widthIn(0.dp, maxWidth).padding(end = 16.dp),
                        contentScale = ContentScale.FillWidth,
                    )
                }
                val textGap = 4.dp
                Column(
                    horizontalAlignment = Alignment.Start,
                ) {
                    if (identityData.firstname != null || identityData.lastname != null) {
                        Text(
                            text = listOfNotNull(
                                identityData.firstname,
                                identityData.lastname
                            ).joinToString(" "),
                            modifier = Modifier.padding(bottom = textGap),
                        )
                    }
                    if (identityData.dateOfBirth != null) {
                        Text(
                            text = identityData.dateOfBirth.run { "$dayOfMonth.$monthNumber.$year" },
                        )
                    }
                }
            }
        }
    }
}