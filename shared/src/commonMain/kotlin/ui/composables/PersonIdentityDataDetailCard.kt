package ui.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.dateOfBirth
import data.firstname
import data.lastname
import data.portrait
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

val List<SubjectCredentialStore.StoreEntry>.preIdentityData: PreIdentityData
    get() = PreIdentityData(
        firstname = firstNotNullOfOrNull { it.firstname },
        lastname = firstNotNullOfOrNull { it.lastname },
        dateOfBirth = firstNotNullOfOrNull { it.dateOfBirth },
        portrait = firstNotNullOfOrNull { it.portrait },
    )

@Composable
fun PersonIdentityDataDetailCard(
    identityData: IdentityData,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
//            PersonAttributeDetailCardHeading(
//                avatarText = "ID",
//                title = "Identitätsdaten",
//            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(top = 12.dp, end = 4.dp, bottom = 12.dp, start = 16.dp)
                    .fillMaxWidth(),
            ) {
//                AvatarHeading(
//                    avatarText = "ID",
//                    title = "Identitätsdaten",
//                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
//                    TextAvatar(
//                        text = "ID",
//                    )
                    IconButton(
                        onClick = {},
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = contentColorFor(MaterialTheme.colorScheme.secondaryContainer),
                            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            disabledContentColor = contentColorFor(MaterialTheme.colorScheme.secondaryContainer),
                        ),
                        enabled = false,
                    ) {
                        if (identityData.portrait != null) {
                            Image(
                                bitmap = identityData.portrait,
                                contentDescription = "Picture",
                            )
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "ID",
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Identitätsdaten",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Unspecified,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Row(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            ) {
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
                            modifier = Modifier.padding(bottom = textGap),
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp - textGap))
                }
            }
        }
    }
}