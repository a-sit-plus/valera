package ui.views.authentication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_continue
import at.asitplus.valera.resources.info_text_error_action_start_screen
import at.asitplus.valera.resources.info_text_no_matching_credential
import org.jetbrains.compose.resources.stringResource
import ui.viewmodels.authentication.AuthenticationNoCredentialViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticationNoCredentialView(vm: AuthenticationNoCredentialViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                }
            )
        },
        bottomBar = {
            Surface(
                color = NavigationBarDefaults.containerColor,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.info_text_error_action_start_screen),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Button(onClick = vm.navigateToHomeScreen) {
                            Text(stringResource(Res.string.button_label_continue))
                        }
                    }
                }
            }
        }
    ) {
        Box(modifier = Modifier.padding(it)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Text(
                    stringResource(Res.string.info_text_no_matching_credential),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

}