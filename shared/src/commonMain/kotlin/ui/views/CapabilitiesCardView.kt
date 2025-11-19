package ui.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.content_description_navigate_to_settings
import at.asitplus.valera.resources.heading_label_capabilities
import at.asitplus.valera.resources.info_text_capabilities_continue
import at.asitplus.valera.resources.info_text_capabilities_missing_capabilities
import org.jetbrains.compose.resources.stringResource
import ui.composables.CapabilityCard
import ui.composables.Logo
import ui.composables.buttons.NavigateUpButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapabilitiesCardView(
    list: Set<CapabilityCardData>,
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    navigateUp: (() -> Unit)? = null
) {
    Scaffold(topBar = {
        TopAppBar(title = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.heading_label_capabilities),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineMedium,
                    fontSize = 20.sp
                )
            }
        }, actions = {
            Logo(onClick = onClickLogo)
            Column(modifier = Modifier.clickable(onClick = onClickSettings)) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(Res.string.content_description_navigate_to_settings),
                )
            }
            Spacer(Modifier.width(15.dp))
        },
            navigationIcon = {
                navigateUp?.let {
                    NavigateUpButton(it)
                }
            })
    }, bottomBar = {
        BottomAppBar {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                Text(
                    text = stringResource(Res.string.info_text_capabilities_missing_capabilities),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(Res.string.info_text_capabilities_continue),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {

                }
            }
        }
    }) { scaffoldPadding ->
        Column(
            modifier = Modifier.padding(scaffoldPadding).fillMaxSize(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, start = 15.dp, end = 15.dp),
            ) {
                list.filter{ !it.success }.forEach {
                    CapabilityCard(it.text, it.success, it.info, it.action)
                    Spacer(Modifier.height(15.dp))
                }
            }
        }
    }
}