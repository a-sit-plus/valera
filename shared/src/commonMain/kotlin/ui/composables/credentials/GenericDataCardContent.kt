package ui.composables.credentials

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.lib.data.ConstantIndex
import data.AttributeTranslator
import org.jetbrains.compose.resources.stringResource
import ui.composables.LabeledContent


@Composable
fun GenericDataCardContent(
    credentialScheme: ConstantIndex.CredentialScheme,
    attributes: List<Pair<NormalizedJsonPath, @Composable () -> Unit>>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        attributes.mapIndexed { index, it ->
            LabeledContent(
                label = AttributeTranslator(credentialScheme).translate(it.first)?.let {
                    stringResource(it)
                } ?: it.first.toString(),
                content = it.second,
                modifier = if (index == attributes.lastIndex) {
                    Modifier
                } else {
                    Modifier.padding(bottom = 8.dp)
                }
            )
        }
    }
}
