package ui.composables.credentials

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.idaustria.IdAustriaScheme
import data.PersonalDataCategory
import data.credentials.IdAustriaCredentialAdapter
import ui.composables.AttributeRepresentation

@Composable
fun IdAustriaCredentialResidenceDataCard(
    credentialAdapter: IdAustriaCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    CredentialDetailCard(
        credentialScheme = IdAustriaScheme,
        personalDataCategory = PersonalDataCategory.ResidenceData,
        credentialAdapter = credentialAdapter,
        modifier = modifier,
    ) {
        IdAustriaCredentialResidenceDataCardContent(
            credentialAdapter = credentialAdapter,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )
    }
}

@Composable
fun IdAustriaCredentialResidenceDataCardContent(
    credentialAdapter: IdAustriaCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
    ) {
        listOfNotNull(
            credentialAdapter.mainAddress?.street,
            credentialAdapter.mainAddress?.houseNumber,
            credentialAdapter.mainAddress?.stair,
            credentialAdapter.mainAddress?.door,
        ).any { it.isNotBlank() }.let {
            if (it) {
                val untilHouseNumber = listOfNotNull(
                    credentialAdapter.mainAddress?.street,
                    credentialAdapter.mainAddress?.houseNumber,
                ).filter {
                    it.isNotBlank()
                }.joinToString(" ")

                val afterHouseNumber = listOfNotNull(
                    credentialAdapter.mainAddress?.stair,
                    credentialAdapter.mainAddress?.door,
                ).filter {
                    it.isNotBlank()
                }.joinToString("/")

                val firstLine = listOfNotNull(
                    untilHouseNumber,
                    afterHouseNumber,
                ).filter {
                    it.isNotBlank()
                }.joinToString("/")

                AttributeRepresentation(firstLine)
            }
        }

        listOfNotNull(
            credentialAdapter.mainAddress?.postalCode,
            credentialAdapter.mainAddress?.locality,
        ).any { it.isNotBlank() }.let {
            if (it) {
                AttributeRepresentation(
                    value = listOfNotNull(
                        credentialAdapter.mainAddress?.postalCode,
                        credentialAdapter.mainAddress?.locality,
                    ).filter { it.isNotBlank() }.joinToString(" "),
                )
            }
        }
    }
}