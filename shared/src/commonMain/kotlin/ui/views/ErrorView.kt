package ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_ok
import at.asitplus.valera.resources.heading_label_error_screen
import at.asitplus.valera.resources.info_text_error_occurred
import org.jetbrains.compose.resources.stringResource
import ui.composables.Logo
import ui.composables.ScreenHeading
import ui.composables.TextIconButton
import ui.viewmodels.ErrorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorView(
    vm: ErrorViewModel
) {
    val tint = Color(255, 210, 0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        ScreenHeading(stringResource(Res.string.heading_label_error_screen), Modifier.weight(1f))
                    }
                },
                actions = {
                    Logo(onClick = vm.onClickLogo)
                    Column(modifier = Modifier.clickable(onClick = vm.onClickSettings)) {
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
                        text = stringResource(Res.string.info_text_error_occurred),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(vm.actionDescription),
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
                                Text(stringResource(Res.string.button_label_ok))
                            },
                            onClick = vm.onClickButton,
                        )
                    }
                }
            }
        }
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier.padding(scaffoldPadding).fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp).fillMaxSize()
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    Modifier.size(100.dp),
                    tint = tint
                )
                Text("Message:", fontWeight = FontWeight.Bold)
                Column(
                    modifier = Modifier.heightIn(max = 150.dp)
                        .background(color = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Text(
                        vm.message ?: "Unknown Message",
                        modifier = Modifier.padding(
                            top = 5.dp,
                            bottom = 5.dp,
                            start = 10.dp,
                            end = 10.dp
                        ).fillMaxWidth().verticalScroll(
                            rememberScrollState()
                        ),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.size(5.dp))
                vm.textCause?.let {
                    Text("Cause:", fontWeight = FontWeight.Bold)
                    Column(
                        modifier = Modifier.heightIn(max = 150.dp)
                            .background(color = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Text(
                            it,
                            modifier = Modifier.padding(
                                top = 5.dp,
                                bottom = 5.dp,
                                start = 10.dp,
                                end = 10.dp
                            ).fillMaxWidth().verticalScroll(
                                rememberScrollState()
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
