package ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.CredentialExtractor
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource

data class AgeData(
    val ageLowerBounds: List<Int> = listOf(),
    val ageUpperBounds: List<Int> = listOf(),
)

val CredentialExtractor.ageData: AgeData
    get() = AgeData(
        ageLowerBounds = listOfNotNull(
            if(ageAtLeast14 == true) 14 else null,
            if(ageAtLeast16 == true) 16 else null,
            if(ageAtLeast18 == true) 18 else null,
            if(ageAtLeast21 == true) 21 else null,
        ),
        ageUpperBounds = listOfNotNull(
            if(ageAtLeast14 == false) 14 else null,
            if(ageAtLeast16 == false) 16 else null,
            if(ageAtLeast18 == false) 18 else null,
            if(ageAtLeast21 == false) 21 else null,
        ),
    )

@OptIn(ExperimentalLayoutApi::class, ExperimentalResourceApi::class)
@Composable
fun PersonAgeDataDetailCard(
    ageData: AgeData,
    onClickOpenDetails: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            PersonAttributeDetailCardHeading(
                iconText = stringResource(PersonalDataCategory.AgeData.iconText),
                title = stringResource(PersonalDataCategory.AgeData.categoryTitle),
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

            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
            ) {
                for (ageString in ageData.ageLowerBounds.sorted().map { "≥$it" }) {
                    SuggestionChip(
                        label = {
                            Text(text = ageString)
                        },
                        onClick = {},
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
            ) {
                for (ageString in ageData.ageUpperBounds.sorted().map { "<$it" }) {
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