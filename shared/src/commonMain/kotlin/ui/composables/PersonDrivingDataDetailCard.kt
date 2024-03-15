package ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource

data class DrivingData(
    val drivingPermissions: List<String>
)
@OptIn(ExperimentalLayoutApi::class, ExperimentalResourceApi::class)
@Composable
fun PersonDrivingDataDetailCard(
    drivingData: DrivingData,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            PersonAttributeDetailCardHeading(
                iconText = stringResource(PersonalDataCategory.DrivingPermissions.iconText),
                title = stringResource(PersonalDataCategory.DrivingPermissions.categoryTitle),
            )

            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
            ) {
                for (ageString in drivingData.drivingPermissions.sorted()) {
                    SuggestionChip(
                        label = {
                            Text(text = ageString)
                        },
                        onClick = {},
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
