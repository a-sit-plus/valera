package ui.composables.credentials

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.idaustria.IdAustriaScheme
import data.AttributeTranslator
import data.PersonalDataCategory
import data.credentials.IdAustriaCredentialAdapter
import org.jetbrains.compose.resources.stringResource
import ui.composables.AttributeRepresentation

@Composable
fun IdAustriaAgeDataCard(
    credentialAdapter: IdAustriaCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    var isExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    val genericFieldRepresentations = genericAgeFieldRepresentations(credentialAdapter)

    if (genericFieldRepresentations.isNotEmpty()) {
        IdAustriaAgeDataCard(
            isExpanded = isExpanded,
            onChangeIsExpanded = { isExpanded = !isExpanded },
            credentialAdapter = credentialAdapter,
            genericFieldRepresentations = genericFieldRepresentations,
            modifier = modifier,
        )
    }
}

@Composable
fun IdAustriaAgeDataCard(
    isExpanded: Boolean,
    onChangeIsExpanded: (Boolean) -> Unit,
    credentialAdapter: IdAustriaCredentialAdapter,
    genericFieldRepresentations: List<Pair<String, @Composable () -> Unit>>,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            CredentialDetailCardHeader(
                iconText = PersonalDataCategory.AgeData.iconText,
                title = PersonalDataCategory.AgeData.categoryTitle,
                isExpanded = isExpanded,
                onChangeIsExpanded = onChangeIsExpanded,
            )
            if (isExpanded) {
                GenericDataCardContent(
                    genericFieldRepresentations,
                    modifier = Modifier.padding(bottom = 16.dp, end = 16.dp, start = 16.dp)
                )
            } else {
                IdAustriaAgeDataCardContent(
                    credentialAdapter = credentialAdapter,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IdAustriaAgeDataCardContent(
    credentialAdapter: IdAustriaCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    val agesAtLeastN = listOf(
        14 to credentialAdapter.ageAtLeast14,
        16 to credentialAdapter.ageAtLeast16,
        18 to credentialAdapter.ageAtLeast18,
        21 to credentialAdapter.ageAtLeast21,
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
    }
}

@Composable
private fun genericAgeFieldRepresentations(credentialAdapter: IdAustriaCredentialAdapter): List<Pair<String, @Composable () -> Unit>> =
    credentialAdapter.run {
        listOfNotNull<Pair<String, @Composable () -> Unit>>(
            ageAtLeast14?.let {
                IdAustriaScheme.Attributes.AGE_OVER_14 to {
                    AttributeRepresentation(it)
                }
            },
            ageAtLeast16?.let {
                IdAustriaScheme.Attributes.AGE_OVER_16 to {
                    AttributeRepresentation(it)
                }
            },
            ageAtLeast18?.let {
                IdAustriaScheme.Attributes.AGE_OVER_18 to {
                    AttributeRepresentation(it)
                }
            },
            ageAtLeast21?.let {
                IdAustriaScheme.Attributes.AGE_OVER_21 to {
                    AttributeRepresentation(it)
                }
            },
        ).map {
            val translation =
                AttributeTranslator(IdAustriaScheme).translate(it.first)?.let {
                    stringResource(it)
                } ?: it.first
            translation to it.second
        }
    }