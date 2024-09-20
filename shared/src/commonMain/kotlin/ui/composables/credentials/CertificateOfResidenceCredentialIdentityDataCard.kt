package ui.composables.credentials

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.content_description_portrait
import data.PersonalDataCategory
import data.credentials.CertificateOfResidenceCredentialAdapter
import data.credentials.IdAustriaCredentialAdapter
import org.jetbrains.compose.resources.stringResource
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