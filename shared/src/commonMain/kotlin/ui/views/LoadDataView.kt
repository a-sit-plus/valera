package ui.views

import Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.composables.BiometryPrompt
import ui.composables.OutlinedTextIconButton
import ui.composables.buttons.LoadDataButton
import ui.composables.buttons.NavigateUpButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadDataView(
    navigateUp: (() -> Unit)? = null,
    loadData: () -> Unit,
    navigateToQrCodeCredentialProvisioningPage: () -> Unit,
) {
    var showBiometry by rememberSaveable {
        mutableStateOf(false)
    }

    if (showBiometry) {
        BiometryPrompt(
            title = Resources.BIOMETRIC_AUTHENTICATION_PROMPT_TO_LOAD_DATA_TITLE,
            subtitle = Resources.BIOMETRIC_AUTHENTICATION_PROMPT_TO_LOAD_DATA_SUBTITLE,
            onSuccess = loadData,
            onDismiss = {
                showBiometry = false
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = Resources.HEADING_LABEL_LOAD_DATA,
                        style = MaterialTheme.typography.headlineLarge,
                    )
                },
                navigationIcon = {
                    if (navigateUp != null) {
                        NavigateUpButton(navigateUp)
                    }
                },
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    OutlinedTextIconButton(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                            )
                        },
                        text = {
                            Text(Resources.BUTTON_LABEL_SCAN_QR_CODE)
                        },
                        onClick = navigateToQrCodeCredentialProvisioningPage,
                    )
                    LoadDataButton(
                        onClick = {
                            showBiometry = true
                        }
                    )
                }
            }
        }
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier.padding(scaffoldPadding)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
        ) {
            Text(
                Resources.INFO_TEXT_REDICRECTION_TO_ID_AUSTRIA_FOR_CREDENTIAL_PROVISIONING,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                Resources.INFO_TEXT_NOTICE_DEVELOPMENT_PROVISIONING_USING_QR_CODE_CREDENTIALS,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}
