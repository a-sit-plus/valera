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
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.text_label_door
import composewalletapp.shared.generated.resources.text_label_stair
import data.PersonalDataCategory
import data.credentials.IdAustriaCredentialAdapter
import org.jetbrains.compose.resources.stringResource

@Composable
fun IdAustriaResidenceDataCard(
    credentialAdapter: IdAustriaCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    CredentialDetailCard(
        credentialScheme = IdAustriaScheme,
        personalDataCategory = PersonalDataCategory.ResidenceData,
        credentialAdapter = credentialAdapter,
        modifier = modifier,
    ) {
        IdAustriaResidenceDataCardContent(
            credentialAdapter = credentialAdapter,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IdAustriaResidenceDataCardContent(
    credentialAdapter: IdAustriaCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        if (
            listOfNotNull(
                credentialAdapter.mainAddress?.street,
                credentialAdapter.mainAddress?.houseNumber,
            ).any { it.isNotBlank() }
        ) {
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

            Text(firstLine)
        }

        if (
            listOfNotNull(
                credentialAdapter.mainAddress?.postalCode,
                credentialAdapter.mainAddress?.locality,
            ).any { it.isNotBlank() }
        ) {
            Text(
                text = listOfNotNull(
                    credentialAdapter.mainAddress?.postalCode,
                    credentialAdapter.mainAddress?.locality,
                ).filter { it.isNotBlank() }.joinToString(" "),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}