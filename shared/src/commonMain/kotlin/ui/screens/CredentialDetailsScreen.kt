package ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.third_party.at.asitplus.wallet.lib.agent.representation
import at.asitplus.wallet.app.common.third_party.at.asitplus.wallet.lib.data.uiLabel
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import at.asitplus.wallet.por.PowerOfRepresentationScheme
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.heading_label_credential_details_screen
import data.credentialAttributeCategorization
import data.storage.StoreEntryId
import org.jetbrains.compose.resources.stringResource
import ui.composables.LabeledText
import ui.composables.buttons.NavigateUpButton
import ui.composables.credentials.CertificateOfResidenceCredentialView
import ui.composables.credentials.EuPidCredentialView
import ui.composables.credentials.GenericCredentialSummaryCardContent
import ui.composables.credentials.IdAustriaCredentialView
import ui.composables.credentials.MobileDrivingLicenceCredentialView
import ui.composables.credentials.PowerOfRepresentationCredentialView

@Composable
fun CredentialDetailsScreen(
    storeEntryId: StoreEntryId,
    navigateUp: () -> Unit,
    walletMain: WalletMain,
) {
    CredentialDetailsScreen(
        navigateUp = navigateUp,
        viewModel = CredentialDetailsScreenViewModel(
            storeEntryId = storeEntryId,
            walletMain = walletMain,
        ),
        imageDecoder = walletMain.platformAdapter::decodeImage
    )
}

@Composable
fun CredentialDetailsScreen(
    navigateUp: () -> Unit,
    viewModel: CredentialDetailsScreenViewModel,
    imageDecoder: (ByteArray) -> ImageBitmap,
) {
    val storeEntry by viewModel.storeEntry.collectAsState(null)

    CredentialDetailsScaffold(
        isStoreEntryAvailable = storeEntry != null,
        navigateUp = navigateUp,
        onDelete = {
            viewModel.deleteStoreEntry()
            navigateUp()
        },
    ) {
        storeEntry?.let {
            CredentialDetailsView(
                storeEntry = it,
                imageDecoder = imageDecoder,
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
                    Text(
                        stringResource(Res.string.heading_label_credential_details_screen),
                        style = MaterialTheme.typography.headlineLarge
                    )
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
fun CredentialDetailsView(
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

            else -> {}
        }
        GenericCredentialSummaryCardContent(
            credential = storeEntry,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}