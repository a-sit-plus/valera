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
    val agesAtLeastN = listOf(
        12 to credentialAdapter.ageAtLeast12,
        13 to credentialAdapter.ageAtLeast13,
        14 to credentialAdapter.ageAtLeast14,
        16 to credentialAdapter.ageAtLeast16,
        18 to credentialAdapter.ageAtLeast18,
        21 to credentialAdapter.ageAtLeast21,
        25 to credentialAdapter.ageAtLeast25,
        60 to credentialAdapter.ageAtLeast60,
        62 to credentialAdapter.ageAtLeast62,
        65 to credentialAdapter.ageAtLeast65,
        68 to credentialAdapter.ageAtLeast68,
    )

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
