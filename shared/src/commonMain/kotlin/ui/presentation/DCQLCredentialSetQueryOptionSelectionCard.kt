package ui.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Card
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.uiLabel

@Composable
fun DCQLCredentialSetQueryOptionSelectionCard(
    isSatisfiable: Boolean,
    credentialQueryUiModels: List<DCQLCredentialQueryUiModel>,
    isSelected: Boolean,
    onSelectCredentialQuery: () -> Unit,
) {
    Card(
        enabled = isSatisfiable,
        onClick = onSelectCredentialQuery,
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelectCredentialQuery,
                enabled = isSatisfiable,
            )
            Column {
                credentialQueryUiModels.forEach {
                    CredentialSetQueryOptionSelectionCardCredentialQueryContent(
                        credentialRepresentationLocalized = it.credentialRepresentationLocalized,
                        credentialSchemeLocalized = it.credentialSchemeLocalized,
                        credentialAttributesLocalized = it.requestedAttributesLocalized?.let {
                            it.attributesLocalized to it.otherAttributes
                        },
                    )
                }
            }
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