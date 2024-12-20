package ui.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import at.asitplus.wallet.eprescription.EPrescriptionScheme
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import at.asitplus.wallet.por.PowerOfRepresentationScheme
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.asp
import at.asitplus.valera.resources.heading_label_authentication_success
import at.asitplus.valera.resources.heading_label_credential_details_screen
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ui.composables.buttons.NavigateUpButton
import ui.composables.credentials.CertificateOfResidenceCredentialView
import ui.composables.credentials.EPrescriptionView
import ui.composables.credentials.EuPidCredentialView
import ui.composables.credentials.GenericCredentialSummaryCardContent
import ui.composables.credentials.IdAustriaCredentialView
import ui.composables.credentials.MobileDrivingLicenceCredentialView
import ui.composables.credentials.PowerOfRepresentationCredentialView
import ui.composables.CredentialCardActionMenu
import ui.viewmodels.CredentialDetailsViewModel

@Composable
fun CredentialDetailsView(
    vm: CredentialDetailsViewModel,
) {
    val storeEntry by vm.storeEntry.collectAsState(null)

    CredentialDetailsScaffold(
        isStoreEntryAvailable = storeEntry != null,
        navigateUp = vm.navigateUp,
        onDelete = {
            vm.deleteStoreEntry()
            vm.navigateUp()
        },
    ) {
        storeEntry?.let {
            CredentialDetailsSummaryView(
                storeEntry = it,
                imageDecoder = vm.imageDecoder,
            )
        } ?: Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxSize(0.5f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialDetailsScaffold(
    isStoreEntryAvailable: Boolean,
    navigateUp: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    NavigateUpButton(navigateUp)
                },
                title = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(Res.string.heading_label_credential_details_screen),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Image(
                            modifier = Modifier.padding(start = 0.dp, end = 8.dp, top = 8.dp),
                            painter = painterResource(Res.drawable.asp),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                        )
                    }
                },
                actions = {
                    if (isStoreEntryAvailable) {
                        CredentialCardActionMenu(
                            onDelete = onDelete
                        )
                    }
                },
            )
        },
        modifier = modifier,
    ) {
        Box(modifier = Modifier.padding(it).verticalScroll(rememberScrollState())) {
            content()
        }
    }
}

@Composable
fun CredentialDetailsSummaryView(
    storeEntry: SubjectCredentialStore.StoreEntry,
    imageDecoder: (ByteArray) -> ImageBitmap,
) {
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        when (storeEntry.scheme) {
            is IdAustriaScheme -> IdAustriaCredentialView(
                credential = storeEntry,
                imageDecoder = imageDecoder,
            )

            is EuPidScheme -> EuPidCredentialView(
                credential = storeEntry,
            )

            is MobileDrivingLicenceScheme -> MobileDrivingLicenceCredentialView(
                credential = storeEntry,
                decodeImage = imageDecoder,
            )

            is PowerOfRepresentationScheme -> PowerOfRepresentationCredentialView(
                credential = storeEntry,
            )

            is CertificateOfResidenceScheme -> CertificateOfResidenceCredentialView(
                credential = storeEntry,
            )

            is EPrescriptionScheme -> EPrescriptionView(
                credential = storeEntry
            )

            else -> {}
        }
        GenericCredentialSummaryCardContent(
            credential = storeEntry,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}
