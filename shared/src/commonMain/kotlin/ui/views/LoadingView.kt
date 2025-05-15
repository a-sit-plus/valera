package ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.heading_label_loading_screen
import org.jetbrains.compose.resources.stringResource
import ui.composables.ScreenHeading
import ui.composables.buttons.NavigateUpButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadingView(
    customLabel: String = "",
    navigateUp: (() -> Unit)? = null
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    ScreenHeading(stringResource(Res.string.heading_label_loading_screen))
                },
                navigationIcon = {
                    navigateUp?.let { NavigateUpButton(onClick = navigateUp) }
                }
            )
        }
    ) { scaffoldPadding ->
        LoadingViewBody(scaffoldPadding, customLabel)
    }
}

@Composable
fun LoadingViewBody(
    scaffoldPadding: PaddingValues,
    customLabel: String = ""
) {
    Column(
        modifier = Modifier.padding(scaffoldPadding).fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxSize(0.5f)
        )
        Text(
            text = customLabel,
            style = MaterialTheme.typography.labelLarge
        )
    }
}