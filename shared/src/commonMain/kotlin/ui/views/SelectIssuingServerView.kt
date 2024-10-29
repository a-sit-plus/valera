package ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.lib.data.ConstantIndex
import ui.composables.buttons.LoadDataButton
import ui.composables.forms.StatefulLoadDataForm
import ui.composables.forms.StatefulSelectIssuingServerForm

@Composable
fun SelectIssuingServerView(
    // state
    host: TextFieldValue,
    onChangeHost: ((TextFieldValue) -> Unit)?,
    // other
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    LoadDataButton(
                        onClick = onSubmit
                    )
                }
            }
        },
        modifier = modifier,
    ) { scaffoldPadding ->
        StatefulSelectIssuingServerForm(
            host = host,
            onChangeHost = onChangeHost,
            modifier = Modifier.padding(scaffoldPadding),
        )
    }
}
