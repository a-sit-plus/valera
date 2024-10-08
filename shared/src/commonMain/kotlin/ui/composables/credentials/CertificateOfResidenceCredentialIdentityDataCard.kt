package ui.composables.credentials

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import data.PersonalDataCategory
import data.credentials.CertificateOfResidenceCredentialAdapter
import ui.composables.AttributeRepresentation

@Composable
fun CertificateOfResidenceCredentialIdentityDataCard(
    credentialAdapter: CertificateOfResidenceCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    CredentialDetailCard(
        credentialScheme = CertificateOfResidenceScheme,
        personalDataCategory = PersonalDataCategory.IdentityData,
        credentialAdapter = credentialAdapter,
        modifier = modifier,
    ) {
        CertificateOfResidenceCredentialIdentityDataCardContent(
            credentialAdapter = credentialAdapter,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        )
    }
}

@Composable
    fun CertificateOfResidenceCredentialIdentityDataCardContent(
    credentialAdapter: CertificateOfResidenceCredentialAdapter,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier,
    ) {
        val spacingModifier = Modifier.padding(bottom = 4.dp)
        if (credentialAdapter.givenName != null || credentialAdapter.familyName != null) {
            AttributeRepresentation(
                value = listOfNotNull(
                    credentialAdapter.givenName,
                    credentialAdapter.familyName
                ).joinToString(" "),
                modifier = spacingModifier,
            )
        }
        credentialAdapter.birthDate?.let {
            AttributeRepresentation(it, modifier = spacingModifier)
        }
    }
}
