package ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.CredentialExtractor

data class ResidenceData(
    val mainAddress: String?,
)

val CredentialExtractor.residenceData: ResidenceData
    get() = ResidenceData(
        mainAddress = this.mainAddress,
    )

@Composable
fun PersonResidenceDataDetailCard(
    residenceData: ResidenceData,
    onClickOpenDetails: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            PersonAttributeDetailCardHeading(
                iconText = PersonalDataCategory.ResidenceData.iconText,
                title = PersonalDataCategory.ResidenceData.categoryTitle,
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

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                val textGap = 4.dp
                if (residenceData.mainAddress != null) {
                    Text(
                        text = residenceData.mainAddress,
                        modifier = Modifier.padding(bottom = textGap),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp - textGap))
            }
        }
    }
}
