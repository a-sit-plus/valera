package ui.composables.credentials

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import data.PersonalDataCategory
import data.credentials.MobileDrivingLicenceCredentialAdapter
import ui.composables.AttributeRepresentation

@Composable
fun MobileDrivingLicenceCredentialAgeDataCard(
    credentialAdapter: MobileDrivingLicenceCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    CredentialDetailCard(
        credentialScheme = MobileDrivingLicenceScheme,
        personalDataCategory = PersonalDataCategory.AgeData,
        credentialAdapter = credentialAdapter,
        modifier = modifier,
    ) {
        MobileDrivingLicenceCredentialAgeDataCardContent(
            credentialAdapter = credentialAdapter,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MobileDrivingLicenceCredentialAgeDataCardContent(
    credentialAdapter: MobileDrivingLicenceCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    val agesAtLeastN = listOf(
        18 to credentialAdapter.ageAtLeast18,
    )

    Column(modifier = modifier) {
        listOfNotNull(
            credentialAdapter.ageInYears,
            credentialAdapter.ageBirthYear,
        ).let {
            AttributeRepresentation(
                value = listOfNotNull(
                    credentialAdapter.ageInYears,
                    credentialAdapter.ageBirthYear,
                ).joinToString(" | "),
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            for (ageString in agesAtLeastN.filter { it.second == true }.map { "≥${it.first}" }) {
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
            modifier = Modifier.fillMaxWidth()
        ) {
            for (ageString in agesAtLeastN.filter { it.second == false }.map { "<${it.first}" }) {
                SuggestionChip(
                    label = {
                        Text(text = ageString)
                    },
                    onClick = {},
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}