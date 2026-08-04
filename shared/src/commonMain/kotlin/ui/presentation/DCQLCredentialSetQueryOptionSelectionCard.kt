package ui.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ColumnScope.DCQLCredentialSetQueryOptionSelectionCard(
    isSatisfiable: Boolean,
    credentialQueryUiModels: List<DCQLCredentialQueryUiModel>,
    isSelected: Boolean,
    onSelectCredentialSetQuery: () -> Unit,
) {
    ElevatedCard(
        onClick = onSelectCredentialSetQuery,
        colors = when {
            isSatisfiable -> CardDefaults.elevatedCardColors()

            /**
             * Let's still allow selecting unsatisfiable ones
             */
            else -> CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            )
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(8.dp)
            ) {
                credentialQueryUiModels.forEachIndexed { index, it ->
                    CredentialSetQueryOptionSelectionCardCredentialQueryContent(
                        credentialRepresentationLocalized = it.credentialRepresentationLocalized,
                        credentialSchemeLocalized = it.credentialSchemeLocalized,
                        credentialAttributesLocalized = it.requestedAttributesLocalized?.let {
                            it.attributesLocalized to it.otherAttributes
                        },
                    )
                }
            }
            RadioButton(
                selected = isSelected,
                onClick = onSelectCredentialSetQuery,
            )
        }
    }
}

//@Composable
////@Preview
//fun DCQLCredentialSetQueryOptionSelectionCardPreview() {
//    var isSelected by rememberSaveable {
//        mutableStateOf(false)
//    }
//    DCQLCredentialSetQueryOptionSelectionCard(
//        isSatisfiable = true,
//        isSelected = isSelected,
//        credentialQueryUiModels = listOf(
//            DCQLCredentialQueryUiModel(
//                credentialIdentifierLocalized = "TestPID",
//                requestedAttributesLocalized = listOf(
//                    "Test Attribute 1",
//                )
//            )
//        ),
//    ) {
//        isSelected = !isSelected
//    }
//}