package ui.composables

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class DrivingData(
    val drivingPermissions: List<String>
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonDrivingDataDetailCard(
    drivingData: DrivingData,
    onDetailClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    PersonAttributeDetailCard(
        personalDataCategory = PersonalDataCategory.DrivingLicenseData,
        onDetailClick = onDetailClick,
        modifier = modifier,
    ) {
        FlowRow {
            for (ageString in drivingData.drivingPermissions.sorted()) {
                SuggestionChip(
                    label = {
                        Text(text = ageString)
                    },
                    onClick = {},
                )
            }
        }
    }
}
