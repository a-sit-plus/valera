package ui.composables.credentials

import androidx.compose.foundation.layout.Column
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
import at.asitplus.wallet.lib.data.ConstantIndex
import data.PersonalDataCategory
import data.credentials.CredentialAdapter

@Composable
fun CredentialDetailCard(
    credentialScheme: ConstantIndex.CredentialScheme,
    personalDataCategory: PersonalDataCategory,
    credentialAdapter: CredentialAdapter,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var isExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    val genericAttributeRepresentations = getGenericAttributeRepresentations(
        credentialScheme = credentialScheme,
        personalDataCategory = personalDataCategory,
        credentialAdapter = credentialAdapter,
    )

    if (genericAttributeRepresentations.isNotEmpty()) {
        ElevatedCard(
            modifier = modifier,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                CredentialDetailCardHeader(
                    dataCategory = personalDataCategory,
                    isExpanded = isExpanded,
                    onChangeIsExpanded = {
                        isExpanded = !isExpanded
                    },
                )
                if (isExpanded) {
                    GenericDataCardContent(
                        credentialScheme = credentialScheme,
                        attributes = genericAttributeRepresentations,
                        modifier = Modifier.padding(bottom = 16.dp, end = 16.dp, start = 16.dp)
                    )
                } else {
                    content()
                }
            }
        }
    }
}