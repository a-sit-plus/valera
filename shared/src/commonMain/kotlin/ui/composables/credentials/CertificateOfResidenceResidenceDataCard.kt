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
fun CertificateOfResidenceResidenceDataCard(
    credentialAdapter: CertificateOfResidenceCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    CredentialDetailCard(
        credentialScheme = CertificateOfResidenceScheme,
        personalDataCategory = PersonalDataCategory.ResidenceData,
        credentialAdapter = credentialAdapter,
        modifier = modifier,
    ) {
        CertificateOfResidenceResidenceDataCardContent(
            credentialAdapter = credentialAdapter,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )
    }
}

@Composable
fun CertificateOfResidenceResidenceDataCardContent(
    credentialAdapter: CertificateOfResidenceCredentialAdapter,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
    ) {
        listOfNotNull(
            credentialAdapter.residenceAddressThoroughfare,
            credentialAdapter.residenceAddressLocatorName,
            credentialAdapter.residenceAddressLocatorDesignator,
            credentialAdapter.residenceAddressPoBox,
        ).any { it.isNotBlank() }.let {
            if (it) {
                val firstAddressLine = listOfNotNull(
                    credentialAdapter.residenceAddressThoroughfare,
                    credentialAdapter.residenceAddressLocatorName,
                    credentialAdapter.residenceAddressLocatorDesignator,
                    credentialAdapter.residenceAddressPoBox,
                ).filter {
                    it.isNotBlank()
                }.joinToString(" ")

                AttributeRepresentation(
                    value = firstAddressLine,
                )
            }
        }

        listOfNotNull(
            credentialAdapter.residenceAddressPostCode,
            credentialAdapter.residenceAddressPostName,
        ).any { it.isNotBlank() }.let {
            if (it) {
                AttributeRepresentation(
                    value = listOfNotNull(
                        credentialAdapter.residenceAddressPostCode,
                        credentialAdapter.residenceAddressPostName,
                    ).filter { it.isNotBlank() }.joinToString(" "),
                )
            }
        }

        listOfNotNull(
            credentialAdapter.residenceAddressAdminUnitL1,
            credentialAdapter.residenceAddressAdminUnitL2,
        ).any { it.isNotBlank() }.let {
            if (it) {
                AttributeRepresentation(
                    value = listOfNotNull(
                        credentialAdapter.residenceAddressAdminUnitL1,
                        credentialAdapter.residenceAddressAdminUnitL2,
                    ).filter { it.isNotBlank() }.joinToString(" "),
                )
            }
        }
    }
}