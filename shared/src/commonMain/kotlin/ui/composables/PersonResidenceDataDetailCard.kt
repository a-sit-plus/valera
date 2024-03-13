package ui.composables

import Resources
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
    val streetName: String?,
    val houseNumber: String?,
    val stairName: String?,
    val doorName: String?,
    val postalCode: String?,
    val townName: String?,
    val stateName: String?,
    val countryName: String?,
)

val CredentialExtractor.residenceData: ResidenceData
    get() = ResidenceData(
        streetName = this.mainResidenceStreetName,
        houseNumber = this.mainResidenceHouseNumber,
        stairName = this.mainResidenceStairName,
        doorName = this.mainResidenceDoorName,
        postalCode = this.mainResidencePostalCode,
        townName = this.mainResidenceTownName,
        stateName = this.mainResidenceStateName,
        countryName = this.mainResidenceCountryName,
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
                if (
                    listOfNotNull(
                        residenceData.streetName,
                        residenceData.houseNumber,
                    ).any { it.isNotBlank() }
                ) {
                    Text(
                        text = listOfNotNull(
                            residenceData.streetName,
                            residenceData.houseNumber,
                        ).filter { it.isNotBlank() }.joinToString(" "),
                        modifier = Modifier.padding(bottom = textGap),
                    )
                }
                if (residenceData.stairName?.isNotBlank() == true) {
                    Text(
                        text = "${Resources.TEXT_LABEL_STAIR}: ${residenceData.stairName}",
                        modifier = Modifier.padding(bottom = textGap),
                    )
                }
                if (residenceData.doorName?.isNotBlank() == true) {
                    Text(
                        text = "${Resources.TEXT_LABEL_DOOR}: ${residenceData.doorName}",
                        modifier = Modifier.padding(bottom = textGap),
                    )
                }
                if (
                    listOfNotNull(
                        residenceData.postalCode,
                        residenceData.townName,
                    ).any { it.isNotBlank() }
                ) {
                    Text(
                        text = listOfNotNull(
                            residenceData.postalCode,
                            residenceData.townName,
                        ).filter { it.isNotBlank() }.joinToString(" "),
                        modifier = Modifier.padding(bottom = textGap),
                    )
                }
                if (residenceData.stateName?.isNotBlank() == true) {
                    Text(
                        text = residenceData.stateName,
                        modifier = Modifier.padding(bottom = textGap),
                    )
                }
                if (residenceData.countryName?.isNotBlank() == true) {
                    Text(
                        text = residenceData.countryName,
                        modifier = Modifier.padding(bottom = textGap),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp - textGap))
            }
        }
    }
}
