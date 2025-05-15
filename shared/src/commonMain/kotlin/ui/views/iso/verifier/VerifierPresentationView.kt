package ui.views.iso.verifier

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.heading_label_received_data
import at.asitplus.valera.resources.section_heading_document_type
import at.asitplus.wallet.app.common.decodeImage
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.healthid.HealthIdScheme
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import data.credentials.EuPidCredentialAdapter
import data.credentials.HealthIdCredentialAdapter
import data.credentials.MobileDrivingLicenceCredentialAdapter
import data.document.RequestDocumentBuilder
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import ui.composables.Logo
import ui.composables.buttons.NavigateUpButton
import ui.composables.credentials.EuPidCredentialView
import ui.composables.credentials.HealthIdView
import ui.composables.credentials.MobileDrivingLicenceCredentialView
import ui.viewmodels.iso.VerifierViewModel
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalEncodingApi::class, ExperimentalResourceApi::class
)
@Composable
fun VerifierPresentationView(vm: VerifierViewModel) {
    val imageDecoder: (ByteArray) -> ImageBitmap = { byteArray ->
        vm.walletMain.platformAdapter.decodeImage(byteArray)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(Res.string.heading_label_received_data),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Logo(onClick = vm.onClickLogo)
                    }
                },
                navigationIcon = { NavigateUpButton(vm.navigateUp) }
            )
        }
    ) { scaffoldPadding ->
        Box(modifier = Modifier.padding(scaffoldPadding)) {
            Column(
                modifier = Modifier.padding(end = 16.dp, start = 16.dp, bottom = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
                vm.deviceResponse.value!!.documents!!.forEach { doc ->
                    val docType = doc.docType
                    Text(
                        text = stringResource(Res.string.section_heading_document_type, docType),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    doc.issuerSigned.namespaces?.forEach { namespace ->

                        val sortedEntries = namespace.value.entries
                            .sortedBy { it.value.elementIdentifier }
                            .associate { it.value.elementIdentifier to it.value.elementValue }

                        val namespaces = mapOf(namespace.key to sortedEntries)

                        val credentialScheme = RequestDocumentBuilder.getDocTypeConfig(docType)?.scheme

                        val credentialAdapter = RequestDocumentBuilder.createAdapterForScheme(
                            credentialScheme, namespaces, imageDecoder
                        )

                        when (credentialScheme) {
                            is EuPidScheme -> EuPidCredentialView(
                                credentialAdapter = credentialAdapter as EuPidCredentialAdapter,
                                decodeImage = imageDecoder
                            )

                            is MobileDrivingLicenceScheme -> MobileDrivingLicenceCredentialView(
                                credentialAdapter = credentialAdapter as MobileDrivingLicenceCredentialAdapter,
                                decodeImage = imageDecoder
                            )

                            is HealthIdScheme -> HealthIdView(
                                credentialAdapter = credentialAdapter as HealthIdCredentialAdapter
                            )

                            else -> {}
                        }
                    }
                }
            }
        }
    }
}
