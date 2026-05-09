package ui.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_ok
import at.asitplus.valera.resources.heading_label_credential_added_screen
import at.asitplus.valera.resources.icon_presentation_success
import at.asitplus.valera.resources.info_text_credential_added
import at.asitplus.valera.resources.info_text_error_action_return_to_invoker
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ui.composables.Logo
import ui.composables.ScreenHeading
import ui.composables.TextIconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialAddedView(
    onAutoDismiss: () -> Unit,
    onClickButton: () -> Unit,
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    isAutoDismissEnabled: Boolean = true,
    credentialContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    var secondsRemaining by remember { mutableIntStateOf(5) }
    var isAcknowledged by remember { mutableStateOf(false) }

    val acknowledge = {
        if (!isAcknowledged) {
            isAcknowledged = true
            onClickButton()
        }
    }

    LaunchedEffect(isAutoDismissEnabled, isAcknowledged, secondsRemaining) {
        if (!isAutoDismissEnabled || isAcknowledged) {
            return@LaunchedEffect
        }
        if (secondsRemaining > 0) {
            delay(1_000)
            if (isAutoDismissEnabled && !isAcknowledged) {
                secondsRemaining--
            }
        } else {
            isAcknowledged = true
            onAutoDismiss()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        ScreenHeading(
                            stringResource(Res.string.heading_label_credential_added_screen),
                            Modifier.weight(1f)
                        )
                    }
                },
                actions = {
                    Logo(onClick = onClickLogo)
                    Column(modifier = Modifier.clickable(onClick = onClickSettings)) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null,
                        )
                    }
                    Spacer(Modifier.width(15.dp))
                },
                navigationIcon = {}
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(
                    BottomAppBarDefaults.ContainerElevation
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.info_text_credential_added),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(Res.string.info_text_error_action_return_to_invoker),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        TextIconButton(
                            icon = {},
                            text = {
                                Text(
                                    if (isAutoDismissEnabled) {
                                        "${stringResource(Res.string.button_label_ok)} ($secondsRemaining)"
                                    } else {
                                        stringResource(Res.string.button_label_ok)
                                    }
                                )
                            },
                            onClick = acknowledge,
                        )
                    }
                }
            }
        }
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier.padding(scaffoldPadding).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.icon_presentation_success),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(160.dp).padding(bottom = 16.dp)
            )
            Text(
                text = stringResource(Res.string.info_text_credential_added),
                style = MaterialTheme.typography.bodyLarge,
            )
            credentialContent?.let {
                Spacer(Modifier.height(24.dp))
                it()
            }
        }
    }
}
