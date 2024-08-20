package ui.composables.credentials

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.idaustria.IdAustriaScheme
import data.PersonalDataCategory
import data.credentials.EuPidCredentialAdapter
import data.credentials.IdAustriaCredentialAdapter

@Composable
fun EuPidCredentialAgeDataCard(
    credentialAdapter: EuPidCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    CredentialDetailCard(
        credentialScheme = IdAustriaScheme,
        personalDataCategory = PersonalDataCategory.AgeData,
        credentialAdapter = credentialAdapter,
        modifier = modifier,
    ) {
        EuPidCredentialAgeDataCardContent(
            credentialAdapter = credentialAdapter,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EuPidCredentialAgeDataCardContent(
    credentialAdapter: EuPidCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    val agesAtLeastN = listOf(
        18 to credentialAdapter.ageAtLeast18,
    )

    Column(modifier = modifier) {
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
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
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
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