package ui.composables.credentials

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.companyregistration.CompanyRegistrationScheme
import data.PersonalDataCategory
import data.credentials.CompanyRegistrationCredentialAdapter
import ui.composables.AttributeRepresentation

@Composable
fun CompanyRegistrationCredentialCompanyDataCard(
    credentialAdapter: CompanyRegistrationCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    CredentialDetailCard(
        credentialScheme = CompanyRegistrationScheme,
        personalDataCategory = PersonalDataCategory.CompanyData,
        credentialAdapter = credentialAdapter,
        modifier = modifier,
    ) {
        CompanyRegistrationCredentialCompanyDataCardContent(
            credentialAdapter = credentialAdapter,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        )
    }
}

@Composable
fun CompanyRegistrationCredentialCompanyDataCardContent(
    credentialAdapter: CompanyRegistrationCredentialAdapter, modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier,
    ) {
        val spacingModifier = Modifier.padding(bottom = 4.dp)
        credentialAdapter.companyName?.let { AttributeRepresentation(it, spacingModifier) }
        credentialAdapter.companyType?.let { AttributeRepresentation(it, spacingModifier) }
        credentialAdapter.companyStatus?.let { AttributeRepresentation(it, spacingModifier) }
        credentialAdapter.companyActivity?.let { AttributeRepresentation(it, spacingModifier) }
    }
}