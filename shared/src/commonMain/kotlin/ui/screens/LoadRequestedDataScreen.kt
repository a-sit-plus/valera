package ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.error_missing_permissions
import composewalletapp.shared.generated.resources.heading_label_request_log
import composewalletapp.shared.generated.resources.heading_label_requested_data
import composewalletapp.shared.generated.resources.info_text_data_is_being_loaded
import data.verifier.Entry
import data.verifier.Verifier
import data.verifier.getVerifier
import io.github.aakira.napier.Napier
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ui.composables.buttons.NavigateUpButton
import ui.navigation.Page
import ui.navigation.RequestedDataLogOutputPage


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadRequestedDataScreen(document: Verifier.Document, payload: String, navigateUp: () -> Unit) {
    var logsState by remember { mutableStateOf(listOf<String>()) }
    var entryState by remember { mutableStateOf(listOf<Entry>()) }
    var currentpage by remember { mutableStateOf<Page>(RequestedDataLogOutputPage()) }

    val verifier: Verifier = remember { getVerifier() }
    var permission by remember { mutableStateOf(false) }
    if (!permission) {
        verifier.getRequirements { b -> permission = b }
    }

    LaunchedEffect(Unit) {
        val updateLog: (String?, String) -> Unit = { TAG, updateLog ->
            logsState += updateLog
            Napier.d(tag= TAG?: "DebugScreen", message = updateLog)
        }

        val updateData: (List<Entry>) -> Unit = { entry ->
            entryState += entry
        }

        updateLog(null, "Start Connection and Data Transfer")
        verifier.verify(payload, document, updateLog, updateData)
    }

    DisposableEffect(Unit) {
        onDispose {
            verifier.disconnect()
        }
    }

    if (!permission) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        NavigateUpButton(navigateUp)
                    },
                )
            },
        ) { scaffoldPadding ->
            Box(modifier = Modifier.padding(scaffoldPadding)) {
                Text(stringResource(Res.string.error_missing_permissions))
            }
        }
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    for (route in listOf(
                        LocalNavigationData.LOG_SCREEN,
                        LocalNavigationData.DATA_SCREEN,
                    )) {
                        NavigationBarItem(
                            icon = route.icon,
                            label = {
                                Text(stringResource(route.title))
                            },
                            onClick = {
                                if (!route.isActive(currentpage)) {
                                    currentpage = route.destination
                                }
                            },
                            selected = route.isActive(currentpage),
                        )
                    }
                }
            },
            modifier = Modifier,
        ) { scaffoldPadding ->
            Box(modifier = Modifier.padding(scaffoldPadding)) {
                when (currentpage) {
                    is RequestedDataLogOutputPage -> {
                        LoadRequestedDataLogOutputView(
                            logsState = logsState,
                            navigateUp = navigateUp,
                        )
                    }

                    is ui.navigation.RequestedDataShowPage -> {
                        LoadRequestedDataView(
                            entryState = entryState,
                            navigateUp = navigateUp,
                        )
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadRequestedDataLogOutputView(logsState: List<String>, navigateUp: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(Res.string.heading_label_request_log),
                            style = MaterialTheme.typography.headlineLarge,
                        )
                    }
                },
                navigationIcon = {
                    NavigateUpButton(navigateUp)
                },
            )
        },
    ) { scaffoldPadding ->
        Box(modifier = Modifier.padding(scaffoldPadding)) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(logsState.size) { index ->
                    Text(text = logsState[index])
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadRequestedDataView(entryState: List<Entry>, navigateUp: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(Res.string.heading_label_requested_data),
                            style = MaterialTheme.typography.headlineLarge,
                        )
                    }
                },
                navigationIcon = {
                    NavigateUpButton(navigateUp)
                },
            )
        },
    ) { scaffoldPadding ->
        Box(modifier = Modifier.padding(scaffoldPadding)) {
            if (entryState.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(Res.string.info_text_data_is_being_loaded))
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(entryState.size) { index ->
                        entryState[index].show()
                    }
                }
            }
        }
    }
}



private enum class LocalNavigationData(
    val title: StringResource,
    val icon: @Composable () -> Unit,
    val destination: Page,
    val isActive: (Page) -> Boolean
) {
    LOG_SCREEN(
        title = Res.string.heading_label_request_log,
        icon = {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = null,
            )
        },
        destination = RequestedDataLogOutputPage(),
        isActive = {
            when (it) {
                is RequestedDataLogOutputPage -> true
                else -> false
            }
        },
    ),
    DATA_SCREEN(
        title = Res.string.heading_label_requested_data,
        icon = {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
            )
        },
        destination = ui.navigation.RequestedDataShowPage(),
        isActive = {
            when (it) {
                is ui.navigation.RequestedDataShowPage -> true
                else -> false
            }
        },
    ),
}

