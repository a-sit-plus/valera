package ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_ok
import at.asitplus.valera.resources.heading_label_credential_added_screen
import at.asitplus.valera.resources.heading_label_credentials_added_screen
import at.asitplus.valera.resources.info_text_credential_added
import at.asitplus.valera.resources.info_text_credentials_added
import at.asitplus.valera.resources.info_text_error_action_return_to_invoker
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ui.composables.Logo
import ui.composables.ScreenHeading
import ui.composables.TextIconButton

/**
 * Confirms that [credentialCount] credentials have been added with a banner, showing [credentialContent] below it,
 * e.g. the summary card of each of them, all of which scrolls if it does not fit. [returnInfoText] tells where the
 * user returns to afterwards.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialAddedView(
    onAutoDismiss: () -> Unit,
    onClickButton: () -> Unit,
    onClickLogo: () -> Unit,
    isAutoDismissEnabled: Boolean = true,
    credentialCount: Int = 1,
    returnInfoText: StringResource = Res.string.info_text_error_action_return_to_invoker,
    credentialContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    var secondsRemaining by remember { mutableIntStateOf(5) }
    var isAcknowledged by remember { mutableStateOf(false) }
    val infoText = stringResource(
        if (credentialCount > 1) Res.string.info_text_credentials_added else Res.string.info_text_credential_added
    )

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
                            stringResource(
                                if (credentialCount > 1) Res.string.heading_label_credentials_added_screen
                                else Res.string.heading_label_credential_added_screen
                            ),
                            Modifier.weight(1f)
                        )
                    }
                },
                actions = {
                    Logo(onClick = onClickLogo)
                    Spacer(Modifier.width(15.dp))
                },
                navigationIcon = {}
            )
        },
        bottomBar = {
            Surface(color = NavigationBarDefaults.containerColor) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) {
                    Text(
                        text = stringResource(returnInfoText),
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
            modifier = Modifier.padding(scaffoldPadding).fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CredentialAddedBanner(infoText)
            credentialContent?.invoke(this)
        }
    }
}

@Composable
private fun CredentialAddedBanner(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}
