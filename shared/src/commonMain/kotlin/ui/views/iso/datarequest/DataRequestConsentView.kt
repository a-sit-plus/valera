package ui.views.iso.datarequest

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialogDefaults.shape
import androidx.compose.material3.Card
import androidx.compose.material3.CardElevation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.heading_label_navigate_back
import at.asitplus.valera.resources.heading_label_show_data
import at.asitplus.valera.resources.prompt_send_above_data
import at.asitplus.valera.resources.unknown
import at.asitplus.valera.resources.verified_badge
import data.bletransfer.util.documentTypeToUILabel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import qrcode.color.Colors
import ui.composables.CategorySelectionRowDefaults.Companion.modifier
import ui.composables.ConsentAttributesSection
import ui.composables.Logo
import ui.composables.buttons.CancelButton
import ui.composables.buttons.ContinueButton
import ui.composables.buttons.NavigateUpButton
import ui.theme.md_theme_dark_onBackground
import ui.viewmodels.iso.datarequest.DataRequestConsentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataRequestConsentView(vm: DataRequestConsentViewModel) {
    val vm = remember { vm }
    vm.walletMain.cryptoService.onUnauthenticated = vm.navigateUp

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(Res.string.heading_label_navigate_back),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Logo()
                    }
                },
                navigationIcon = {
                    NavigateUpButton(vm.navigateUp)
                },
            )
        },
        bottomBar = {
            Surface(
                color = NavigationBarDefaults.containerColor,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.prompt_send_above_data),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CancelButton(vm.navigateUp)
                        Spacer(modifier = Modifier.width(16.dp))
                        ContinueButton(vm.consentToDataTransmission)
                    }
                }
            }
        }
    ) {
        Box(modifier = Modifier.padding(it)) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                val paddingModifier = Modifier.padding(bottom = 32.dp)

                Text(
                    stringResource(Res.string.heading_label_show_data),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = paddingModifier,
                )


                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(state = rememberScrollState()),
                ) {
                    Column {
                        Text(
                            text = "Requester:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            Text(
                                modifier = Modifier.padding(start = 32.dp),
                                text = vm.walletMain.holder.getRequesterIdentity()
                            )
                            val iconPainter = if (vm.walletMain.holder.getRequesterIdentity() != "Anonymous user") {
                                painterResource(Res.drawable.verified_badge)
                            } else {
                                painterResource(Res.drawable.unknown)
                            }
                            Icon(
                                painter = iconPainter,
                                contentDescription = null,
                                modifier = Modifier.size(MaterialTheme.typography.headlineSmall.fontSize.value.dp).align(Alignment.CenterVertically).padding(start = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    vm.requestedAttributes.forEach { requestedDocument ->
                        requestedDocument.nameSpaces.forEach { nameSpace ->
                            ConsentAttributesSection(
                                title = documentTypeToUILabel(requestedDocument.docType),
                                list = nameSpace.attributes.map { it.displayName }
                            )
                        }
                    }
                }
            }
        }
    }
}
