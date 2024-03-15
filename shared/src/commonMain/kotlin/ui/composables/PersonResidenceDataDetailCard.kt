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
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.TEXT_LABEL_DOOR
import composewalletapp.shared.generated.resources.TEXT_LABEL_STAIR
import data.CredentialExtractor
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource

data class ResidenceData(
    val streetName: String?,
    val houseNumber: String?,
    val stairName: String?,
    val doorName: String?,
    val postalCode: String?,
    val villageName: String?,
)

val CredentialExtractor.residenceData: ResidenceData
    get() = ResidenceData(
        streetName = this.mainAddressStreetName,
        houseNumber = this.mainAddressHouseNumber,
        stairName = this.mainAddressStair,
        doorName = this.mainAddressDoor,
        postalCode = this.mainAddressPostalCode,
        villageName = this.mainAddressVillageName,
    )

@OptIn(ExperimentalResourceApi::class)
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
                iconText = stringResource(PersonalDataCategory.ResidenceData.iconText),
                title = stringResource(PersonalDataCategory.ResidenceData.categoryTitle),
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
                    )
                }
                if (residenceData.stairName?.isNotBlank() == true) {
                    Text(
                        text = "${stringResource(Res.string.TEXT_LABEL_STAIR)} ${residenceData.stairName}",
                    )
                }
                if (residenceData.doorName?.isNotBlank() == true) {
                    Text(
                        text = "${stringResource(Res.string.TEXT_LABEL_DOOR)}: ${residenceData.doorName}",
                    )
                }
                if (
                    listOfNotNull(
                        residenceData.postalCode,
                        residenceData.villageName,
                    ).any { it.isNotBlank() }
                ) {
                    Text(
                        text = listOfNotNull(
                            residenceData.postalCode,
                            residenceData.villageName,
                        ).filter { it.isNotBlank() }.joinToString(" "),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
