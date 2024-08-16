package ui.composables.credentials

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.idaustria.IdAustriaScheme
import data.PersonalDataCategory
import data.credentials.IdAustriaCredentialAdapter
import ui.composables.getGenericAttributeRepresentations

@Composable
fun IdAustriaResidenceDataCard(
    credentialAdapter: IdAustriaCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    var isExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    IdAustriaResidenceDataCard(
        isExpanded = isExpanded,
        onChangeIsExpanded = { isExpanded = !isExpanded },
        credentialAdapter = credentialAdapter,
        modifier = modifier,
    )
}

@Composable
fun IdAustriaResidenceDataCard(
    isExpanded: Boolean,
    onChangeIsExpanded: (Boolean) -> Unit,
    credentialAdapter: IdAustriaCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    val personalDataCategory = PersonalDataCategory.ResidenceData

    val genericAttributeRepresentations = getGenericAttributeRepresentations(
        credentialScheme = IdAustriaScheme,
        personalDataCategory = personalDataCategory,
        credentialAdapter = credentialAdapter,
    )

    if(genericAttributeRepresentations.isNotEmpty()) {
        ElevatedCard(
            modifier = modifier,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                CredentialDetailCardHeader(
                    dataCategory = personalDataCategory,
                    isExpanded = isExpanded,
                    onChangeIsExpanded = onChangeIsExpanded,
                )
                if (isExpanded) {
                    GenericDataCardContent(
                        credentialScheme = IdAustriaScheme,
                        attributes = genericAttributeRepresentations,
                        modifier = Modifier.padding(bottom = 16.dp, end = 16.dp, start = 16.dp)
                    )
                } else {
                    IdAustriaResidenceDataCardContent(
                        credentialAdapter = credentialAdapter,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IdAustriaResidenceDataCardContent(
    credentialAdapter: IdAustriaCredentialAdapter,
    modifier: Modifier = Modifier,
) {
}