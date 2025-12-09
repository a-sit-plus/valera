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
import at.asitplus.wallet.ageverification.AgeVerificationScheme
import data.PersonalDataCategory
import data.credentials.AgeVerificationCredentialAdapter

@Composable
fun AgeVerificationCredentialAgeDataCard(
    credentialAdapter: AgeVerificationCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    CredentialDetailCard(
        credentialScheme = AgeVerificationScheme,
        personalDataCategory = PersonalDataCategory.AgeData,
        credentialAdapter = credentialAdapter,
        modifier = modifier,
    ) {
        AgeVerificationCredentialAgeDataCardContent(
            credentialAdapter = credentialAdapter,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgeVerificationCredentialAgeDataCardContent(
    credentialAdapter: AgeVerificationCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    val agesAtLeastN = credentialAdapter.getAgesAtLeastN()

    Column(modifier = modifier) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgeVerificationCredentialAgeDataCardContentOverview(
    credentialAdapter: AgeVerificationCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    val agesAtLeastN = credentialAdapter.getAgesAtLeastN()

    Column(modifier = modifier) {
        FlowRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            val lastToShow = agesAtLeastN.filter { it.second == true }.maxByOrNull { it.first }
                ?: agesAtLeastN.filter { it.second == false }.minByOrNull { it.first }
            lastToShow?.let {
                SuggestionChip(
                    label = {
                        Text(text = lastToShow.let { if (it.second == true) "≥${it.first}" else "<${it.first}" })
                    },
                    onClick = {},
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun AgeVerificationCredentialAdapter.getAgesAtLeastN(): List<Pair<Int, Boolean?>> = listOf(
    12 to ageAtLeast12,
    13 to ageAtLeast13,
    14 to ageAtLeast14,
    16 to ageAtLeast16,
    18 to ageAtLeast18,
    21 to ageAtLeast21,
    25 to ageAtLeast25,
    60 to ageAtLeast60,
    62 to ageAtLeast62,
    65 to ageAtLeast65,
    68 to ageAtLeast68,
)
