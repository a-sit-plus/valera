package ui.views.iso.verifier

import androidx.compose.foundation.Image
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.signum.indispensable.cosef.io.ByteStringWrapper
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.heading_label_received_data
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.lib.iso.IssuerSignedItem
import at.asitplus.wallet.mdl.MobileDrivingLicenceDataElements
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import data.credentials.EuPidCredentialAttributeTranslator
import data.credentials.MobileDrivingLicenceCredentialAttributeTranslator
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.stringResource
import ui.composables.LabeledContent
import ui.composables.Logo
import ui.composables.buttons.NavigateUpButton
import ui.viewmodels.iso.VerifierViewModel
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalEncodingApi::class, ExperimentalResourceApi::class
)
@Composable
fun VerifierPresentationView(vm: VerifierViewModel) {
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
                vm.deviceResponse.value!!.documents!!.forEach {
                    val docType = it.docType
                    Text(
                        text = "DocType: $docType",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    it.issuerSigned.namespaces?.forEach { namespace ->
                        namespace.value.entries.sortedBy { it.value.elementIdentifier }
                            .forEach { entry ->
                                when (docType) {
                                    MobileDrivingLicenceScheme.isoDocType -> showMdlEntry(entry)
                                    EuPidScheme.isoDocType -> showPidEntry(entry)
                                }
                                Spacer(modifier = Modifier.size(4.dp))
                            }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalEncodingApi::class, ExperimentalResourceApi::class)
@Composable
fun showMdlEntry(entry: ByteStringWrapper<IssuerSignedItem>) {
    val elementIdentifier = entry.value.elementIdentifier
    val label = MobileDrivingLicenceCredentialAttributeTranslator.translate(
        NormalizedJsonPath(NormalizedJsonPathSegment.NameSegment(elementIdentifier))
    )?.let { stringResource(it) } ?: elementIdentifier
    if (elementIdentifier == MobileDrivingLicenceDataElements.PORTRAIT ||
        elementIdentifier == MobileDrivingLicenceDataElements.SIGNATURE_USUAL_MARK
    ) {
        val size = when (elementIdentifier) {
            MobileDrivingLicenceDataElements.PORTRAIT -> 200.dp
            MobileDrivingLicenceDataElements.SIGNATURE_USUAL_MARK -> 40.dp
            else -> 0.dp
        }
        val imageAsByteArray = when (val value = entry.value.elementValue) {
            is ByteArray -> value
            is String -> Base64.decode(value)
            else -> null
        }
        showImageLabeledContent(imageAsByteArray, size, label)
    } else {
        showLabeledContent(entry, label)
    }
}

@OptIn(ExperimentalEncodingApi::class, ExperimentalResourceApi::class)
@Composable
fun showPidEntry(entry: ByteStringWrapper<IssuerSignedItem>) {
    val elementIdentifier = entry.value.elementIdentifier
    val label = EuPidCredentialAttributeTranslator.translate(
        NormalizedJsonPath(NormalizedJsonPathSegment.NameSegment(elementIdentifier))
    )?.let { stringResource(it) } ?: elementIdentifier
    if (elementIdentifier == EuPidScheme.Attributes.PORTRAIT) {
        val imageAsByteArray = when (val value = entry.value.elementValue) {
            is ByteArray -> value
            is String -> Base64.decode(value)
            else -> null
        }
        showImageLabeledContent(imageAsByteArray, 200.dp, label)
    } else {
        showLabeledContent(entry, label)
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun showImageLabeledContent(imageAsByteArray: ByteArray?, size: Dp, label: String) {
    imageAsByteArray?.let {
        LabeledContent(
            content = {
                Image(
                    bitmap = it.decodeToImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(size)
                )
            },
            label = label
        )
    }
}

@Composable
fun showLabeledContent(entry: ByteStringWrapper<IssuerSignedItem>, label: String) {
    LabeledContent(
        content = {
            Text(
                text = entry.value.elementValue.prettyToString(),
                overflow = TextOverflow.Clip,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        label = label
    )
}

private fun Any.prettyToString() = when (this) {
    is Array<*> -> contentToString()
    else -> toString()
}
