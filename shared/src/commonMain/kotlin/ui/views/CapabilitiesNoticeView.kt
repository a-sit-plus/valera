package ui.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.content_description_navigate_to_settings
import at.asitplus.valera.resources.heading_label_capabilities
import at.asitplus.valera.resources.info_text_capabilities_missing_capabilities
import at.asitplus.wallet.app.common.LoadingMessageKey
import org.jetbrains.compose.resources.stringResource
import ui.composables.CapabilityCard
import ui.composables.Logo
import ui.composables.buttons.NavigateUpButton

/**
 * Tells the user about an optional capability, which, unlike a route prerequisite, is not required to continue and
 * therefore does not resume a pending route.
 *
 * @param isFulfilled whether the capability is available, `null` while it is being checked
 * @param onOpenSettings lets the user fix the capability, e.g. in the device settings
 * @param onRefresh checks the capability again, e.g. after returning from the device settings
 * @param onContinue leaves the notice, also called once the capability is available
 */
@Composable
fun CapabilitiesNoticeView(
    heading: String,
    info: String,
    isFulfilled: Boolean?,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    onContinue: () -> Unit,
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    onNavigateUp: () -> Unit,
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        onRefresh()
    }

    when (isFulfilled) {
        true -> LaunchedEffect(Unit) { onContinue() }
        false -> CapabilitiesNoticeScaffold(
            heading = heading,
            info = info,
            onOpenSettings = onOpenSettings,
            onContinue = onContinue,
            onClickLogo = onClickLogo,
            onClickSettings = onClickSettings,
            onNavigateUp = onNavigateUp,
        )

        null -> LoadingView(loadingMessageString(LoadingMessageKey.CheckingDeviceRequirements))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CapabilitiesNoticeScaffold(
    heading: String,
    info: String,
    onOpenSettings: () -> Unit,
    onContinue: () -> Unit,
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    onNavigateUp: () -> Unit,
) {
    Scaffold(topBar = {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(Res.string.heading_label_capabilities),
                    style = MaterialTheme.typography.headlineMedium,
                    fontSize = 20.sp
                )
            },
            actions = {
                Logo(onClick = onClickLogo)
                Column(modifier = Modifier.clickable(onClick = onClickSettings)) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(Res.string.content_description_navigate_to_settings),
                    )
                }
                Spacer(Modifier.width(15.dp))
            },
            navigationIcon = { NavigateUpButton(onNavigateUp) },
        )
    }, bottomBar = {
        BottomAppBar {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            ) {
                Text(
                    text = stringResource(Res.string.info_text_capabilities_missing_capabilities),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }) { scaffoldPadding ->
        Column(
            modifier = Modifier.padding(scaffoldPadding).fillMaxSize()
                .padding(top = 16.dp, start = 15.dp, end = 15.dp),
        ) {
            CapabilityCard(heading, success = false, info = info, action = onOpenSettings)
        }
    }
}
