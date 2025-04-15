package ui.views.iso.verifier

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import ui.composables.LabeledContent
import ui.composables.Logo
import ui.composables.buttons.NavigateUpButton
import ui.viewmodels.iso.VerifierViewModel
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
                            "Received Data",
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
                    Text("DocType: ${it.docType}")
                    it.issuerSigned.namespaces?.forEach { namespace ->
                        namespace.value.entries.sortedBy { it.value.elementIdentifier }
                            .forEach { entry ->
                                val elementIdentifier = entry.value.elementIdentifier
                                if (elementIdentifier == "portrait" ||
                                    elementIdentifier == "signature_usual_mark"
                                ) {
                                    val byteArray = entry.value.elementValue as ByteArray
                                    val size = when (elementIdentifier) {
                                        "portrait" -> 200.dp
                                        "signature_usual_mark" -> 40.dp
                                        else -> 0.dp
                                    }
                                    LabeledContent(
                                        content = {
                                            Image(
                                                bitmap = byteArray.decodeToImageBitmap(),
                                                contentDescription = null,
                                                modifier = Modifier.size(size)
                                            )
                                        },
                                        label = elementIdentifier
                                    )
                                } else {
                                    LabeledContent(
                                        content = {
                                            Text(
                                                text = entry.value.elementValue.prettyToString(),
                                                overflow = TextOverflow.Clip,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        },
                                        label = elementIdentifier
                                    )
                                }
                            }
                    }
                }
            }
        }
    }
}

private fun Any.prettyToString() = when (this) {
    is Array<*> -> contentToString()
    else -> toString()
}
