package ui.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
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
import at.asitplus.valera.resources.button_label_authenticate
import at.asitplus.valera.resources.button_label_show_data
import at.asitplus.valera.resources.heading_label_show_data
import at.asitplus.valera.resources.info_text_show_data_situation
import at.asitplus.valera.resources.section_heading_authenticate_at_device_subtitle
import at.asitplus.valera.resources.section_heading_authenticate_at_device_title
import at.asitplus.valera.resources.section_heading_show_data_to_other_citizen_subtitle
import at.asitplus.valera.resources.section_heading_show_data_to_other_citizen_title
import org.jetbrains.compose.resources.stringResource
import ui.composables.Logo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowDataView(
    onNavigateToAuthenticationQrCodeScannerView: () -> Unit,
    onNavigateToShowQrCodeView: () -> Unit,
    onClickLogo: () -> Unit,
    bottomBar: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(Res.string.heading_label_show_data),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Logo(onClick = onClickLogo)
                    Spacer(Modifier.width(8.dp))
                }
            })
        },
        bottomBar = { bottomBar() },
        modifier = Modifier
    ) {
        Column(modifier = Modifier.padding(it).fillMaxSize()) {
            Text(
                stringResource(Res.string.info_text_show_data_situation),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp, 8.dp)
            )
            LazyColumn {
                item {
                    ShowDataSelectionCardView(
                        stringResource(Res.string.section_heading_authenticate_at_device_title),
                        stringResource(Res.string.section_heading_authenticate_at_device_subtitle),
                        stringResource(Res.string.button_label_authenticate),
                        onNavigateToAuthenticationQrCodeScannerView
                    )
                }

                item {
                    ShowDataSelectionCardView(
                        stringResource(Res.string.section_heading_show_data_to_other_citizen_title),
                        stringResource(Res.string.section_heading_show_data_to_other_citizen_subtitle),
                        stringResource(Res.string.button_label_show_data),
                        onNavigateToShowQrCodeView
                    )
                }
            }
        }
    }
}

@Composable
fun ShowDataSelectionCardView(
    sectionHeadingTitle: String,
    sectionHeadingSubtitle: String,
    buttonLabel: String,
    onClick: () -> Unit
) {
    val defaultModifier = Modifier.fillMaxWidth().padding(16.dp, 12.dp)
    ElevatedCard(defaultModifier) {
        Column(defaultModifier) {
            Text(
                text = sectionHeadingTitle,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = sectionHeadingSubtitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = buttonLabel)
            }
        }
    }
}
