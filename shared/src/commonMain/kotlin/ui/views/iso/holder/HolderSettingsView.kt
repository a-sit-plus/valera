package ui.views.iso.holder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_continue
import at.asitplus.valera.resources.heading_label_settings_screen
import at.asitplus.valera.resources.heading_label_show_data
import at.asitplus.valera.resources.info_text_transfer_settings_loading
import org.jetbrains.compose.resources.stringResource
import ui.composables.Logo
import ui.composables.ScreenHeading
import ui.composables.TextIconButton
import ui.composables.buttons.NavigateUpButton
import ui.viewmodels.iso.holder.HolderViewModel
import ui.views.LoadingView
import ui.views.iso.common.TransferOptionsView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolderSettingsView(
    navigateUp: () ->Unit,
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    bottomBar: @Composable () -> Unit,
    vm: HolderViewModel
) {
    val settingsReady by vm.settingsReady.collectAsStateWithLifecycle()

    LaunchedEffect(vm) { vm.initSettings() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            ScreenHeading(stringResource(Res.string.heading_label_show_data))
                            Text(
                                text = stringResource(Res.string.heading_label_settings_screen),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                },
                actions = {
                    Logo(onClick = onClickLogo)
                    Column(modifier = Modifier.clickable(onClick = onClickSettings)) {
                        Icon(Icons.Outlined.Settings, null)
                    }
                    Spacer(Modifier.width(15.dp))
                },
                navigationIcon = { NavigateUpButton(navigateUp) }
            )
        },
        bottomBar = { bottomBar() }
    ) { scaffoldPadding ->
        if (!settingsReady) {
            LoadingView(stringResource(Res.string.info_text_transfer_settings_loading))
        } else {
            Box(modifier = Modifier.padding(scaffoldPadding)) {
                Column(
                    modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.Start
                ) {
                    TransferOptionsView( vm)

                    Spacer(Modifier.height(24.dp))
                    TextIconButton(
                        icon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, null) },
                        text = { Text(stringResource(Res.string.button_label_continue)) },
                        onClick = { vm.onConsentSettings() }
                    )
                }
            }
        }
    }
}
