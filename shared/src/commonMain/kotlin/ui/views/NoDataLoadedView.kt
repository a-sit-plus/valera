package ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.info_text_no_credentials_available
import org.jetbrains.compose.resources.stringResource
import ui.composables.buttons.CancelButton
import ui.composables.buttons.LoadDataButton
import ui.composables.buttons.LoadDataQrButton
import ui.composables.buttons.LoadWithFIIssuerButton

@Composable
fun NoDataLoadedView(
    navigateToFIIssuerPage: () -> Unit,
    navigateToQrAddCredentialsPage: () -> Unit,
) {
    val selection = rememberSaveable{mutableStateOf(false)}

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize(),
    ) {
        if (selection.value) {
            LoadWithFIIssuerButton(navigateToFIIssuerPage)
            Spacer(modifier = Modifier.height(5.dp))
            LoadDataQrButton(navigateToQrAddCredentialsPage)
            Spacer(modifier = Modifier.height(5.dp))
            Spacer(modifier = Modifier.height(20.dp))
            CancelButton(onClick = {selection.value = false})
        } else {
            Text(
                text = stringResource(Res.string.info_text_no_credentials_available),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            LoadDataButton(onClick = {selection.value = true})
        }
    }
}