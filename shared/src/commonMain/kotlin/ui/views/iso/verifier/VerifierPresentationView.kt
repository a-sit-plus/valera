package ui.views.iso.verifier

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
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
import at.asitplus.wallet.app.common.decodeImage
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.iconLabel
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.uiLabel
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.healthid.HealthIdScheme
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialScheme
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import data.credentials.EuPidCredentialIsoMdocAdapter
import data.credentials.HealthIdCredentialIsoMdocAdapter
import data.credentials.MobileDrivingLicenceCredentialIsoMdocAdapter
import data.document.RequestDocumentBuilder
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import ui.composables.LabeledText
import ui.composables.Logo
import ui.composables.PersonAttributeDetailCardHeading
import ui.composables.PersonAttributeDetailCardHeadingIcon
import ui.composables.buttons.NavigateUpButton
import ui.composables.credentials.CredentialCardLayout
import ui.composables.credentials.EuPidCredentialViewFromAdapter
import ui.composables.credentials.HealthIdViewFromAdapter
import ui.composables.credentials.MobileDrivingLicenceCredentialViewFromAdapter
import ui.viewmodels.iso.VerifierViewModel
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalEncodingApi::class, ExperimentalResourceApi::class
)
@Composable
fun VerifierPresentationView(vm: VerifierViewModel) {
    val decodeImage: (ByteArray) -> Result<ImageBitmap> = { vm.walletMain.platformAdapter.decodeImage(it) }

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
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
                vm.deviceResponse.value!!.documents!!.forEach { doc ->
                    doc.issuerSigned.namespaces?.forEach { (namespaceKey, entries) ->
                        val scheme = RequestDocumentBuilder.getDocTypeConfig(doc.docType)?.scheme
                        val sortedEntries = entries.entries
                            .sortedBy { it.value.elementIdentifier }
                            .associate { it.value.elementIdentifier to it.value.elementValue }
                        val namespaces = mapOf(namespaceKey to sortedEntries)
                        IsoMdocCredentialViewForScheme(scheme, namespaces, decodeImage)
                    }
                }
            }
        }
    }
}

@Composable
fun IsoMdocCredentialViewForScheme(
    scheme: CredentialScheme?,
    namespaces: Map<String, Map<String, Any>>,
    decodeImage: (ByteArray) -> Result<ImageBitmap>
) {
    CredentialCardLayout(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = Modifier.padding(end = 16.dp, start = 16.dp, bottom = 16.dp)
    ) {
        PersonAttributeDetailCardHeading(
            icon = { PersonAttributeDetailCardHeadingIcon(scheme.iconLabel()) },
            title = {
                LabeledText(
                    label = ConstantIndex.CredentialRepresentation.ISO_MDOC.uiLabel(),
                    text = scheme.uiLabel()
                )
            }
        )
        when (scheme) {
            is MobileDrivingLicenceScheme -> MobileDrivingLicenceCredentialViewFromAdapter(
                MobileDrivingLicenceCredentialIsoMdocAdapter(namespaces, decodeImage)
            )
            is EuPidScheme -> EuPidCredentialViewFromAdapter(
                EuPidCredentialIsoMdocAdapter(namespaces, decodeImage, scheme)
            )
            is HealthIdScheme -> HealthIdViewFromAdapter(
                HealthIdCredentialIsoMdocAdapter(namespaces)
            )
            else -> throw IllegalArgumentException("Unsupported scheme: $scheme")
        }
    }
}
