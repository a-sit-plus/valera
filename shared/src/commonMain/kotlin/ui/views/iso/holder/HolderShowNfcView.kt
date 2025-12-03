package ui.views.iso.holder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Nfc
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
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.heading_label_device_engagement
import at.asitplus.valera.resources.info_text_device_engagement_nfc
import org.jetbrains.compose.resources.stringResource
import ui.composables.Logo
import ui.composables.ScreenHeading
import ui.composables.buttons.NavigateUpButton
import ui.viewmodels.iso.holder.HolderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolderShowNfcView(
    onClickLogo: () -> Unit,
    vm: HolderViewModel
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            ScreenHeading(stringResource(Res.string.heading_label_device_engagement))
                        }
                    }
                },
                actions = {
                    Logo(onClick = onClickLogo)
                    Spacer(Modifier.width(15.dp))
                },
                navigationIcon = { NavigateUpButton({ vm.onResume() }) }
            )
        }
    ) { scaffoldPadding ->
        Box(modifier = Modifier.padding(scaffoldPadding)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Nfc,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(120.dp)
                    )
                    Text(
                        text = stringResource(Res.string.info_text_device_engagement_nfc),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }
            }
        }
    }
}
