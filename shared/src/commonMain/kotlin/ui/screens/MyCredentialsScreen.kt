package ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.iso.ElementValue
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.content_description_refresh_credentials
import composewalletapp.shared.generated.resources.credential_representation_format_label_mso_mdoc
import composewalletapp.shared.generated.resources.credential_representation_format_label_plain_jwt
import composewalletapp.shared.generated.resources.credential_representation_format_label_sd_jwt
import composewalletapp.shared.generated.resources.heading_label_my_data_screen
import composewalletapp.shared.generated.resources.info_text_no_credentials_available
import io.ktor.http.quote
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import ui.composables.FloatingActionButtonHeightSpacer
import ui.composables.LabeledText
import ui.composables.buttons.LoadDataButton
import ui.composables.inputFields.uiLabel
import ui.views.MyCredentialsView

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun MyCredentialsScreen(
    navigateToRefreshCredentialsPage: () -> Unit,
    walletMain: WalletMain,
) {
    val storeContainerState by walletMain.subjectCredentialStore.observeStoreContainer()
        .collectAsState(null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.heading_label_my_data_screen),
                        style = MaterialTheme.typography.headlineLarge
                    )
                },
            )
        },
        floatingActionButton = {
            storeContainerState?.let { storeContainer ->
                if (storeContainer.credentials.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = navigateToRefreshCredentialsPage,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(Res.string.content_description_refresh_credentials),
                        )
                    }
                }
            }
        }
    ) { scaffoldPadding ->
        Column(modifier = Modifier.padding(scaffoldPadding).fillMaxSize()) {
            storeContainerState?.let { storeContainer ->
                if (storeContainer.credentials.isEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Text(
                            text = stringResource(Res.string.info_text_no_credentials_available),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LoadDataButton(
                            onClick = navigateToRefreshCredentialsPage
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.verticalScroll(state = rememberScrollState())
                    ) {
                        storeContainer.credentials.forEach {
                            SingleCredentialCard(
                                it,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                        // make sufficient scroll space for FAB
                        FloatingActionButtonHeightSpacer()
                    }
                }
            }
        }
    }
}

@Composable
private fun SingleCredentialCard(
    credential: SubjectCredentialStore.StoreEntry,
    modifier: Modifier = Modifier,
) {
    when (credential) {
        is SubjectCredentialStore.StoreEntry.Vc -> SingleVcCredentialCard(
            credential = credential,
            modifier = modifier
        )

        is SubjectCredentialStore.StoreEntry.SdJwt -> SingleSdJwtCredentialView(
            credential = credential,
            modifier = modifier
        )

        is SubjectCredentialStore.StoreEntry.Iso -> SingleIsoCredentialView(
            credential = credential,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun SingleVcCredentialCard(
    credential: SubjectCredentialStore.StoreEntry.Vc,
    modifier: Modifier = Modifier,
) {
    SingleCredentialCardLayout(
        modifier = modifier,
    ) {
        LabeledText(
            text = credential.scheme.uiLabel(),
            label = stringResource(Res.string.credential_representation_format_label_plain_jwt),
        )
        Text(credential.vc.toString())
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun SingleSdJwtCredentialView(
    credential: SubjectCredentialStore.StoreEntry.SdJwt,
    modifier: Modifier = Modifier,
) {
    SingleCredentialCardLayout(
        modifier = modifier,
    ) {
        LabeledText(
            text = credential.scheme.uiLabel(),
            label = stringResource(Res.string.credential_representation_format_label_sd_jwt),
        )
        credential.disclosures.forEach {
            LabeledText(
                text = it.value?.claimValue?.toString() ?: "unknown claim value",
                label = it.value?.claimName ?: "unknown claim name"
            )
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun SingleIsoCredentialView(
    credential: SubjectCredentialStore.StoreEntry.Iso,
    modifier: Modifier = Modifier,
) {
    SingleCredentialCardLayout(
        modifier = modifier,
    ) {
        LabeledText(
            text = credential.scheme.uiLabel(),
            label = stringResource(Res.string.credential_representation_format_label_mso_mdoc),
        )
        credential.issuerSigned.namespaces?.forEach { namespace ->
            namespace.value.entries.forEach { entry ->
                LabeledText(
                    text = entry.value.elementValue.let {
                        it.string
                            ?: it.boolean?.toString()
                            ?: it.drivingPrivilege?.toString()
                            ?: it.date?.toString()
                            ?: it.bytes?.toString()!!
                    },
                    label = "\$[${namespace.key.quote()}][${entry.value.elementIdentifier.quote()}]"
                )
            }
        }
    }
}


@Composable
private fun SingleCredentialCardLayout(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 16.dp
            ).fillMaxWidth(),
        ) {
            content()
        }
    }
}

private fun ElementValue.toUiString(): String {
    return this.string
        ?: this.boolean?.toString()
        ?: this.drivingPrivilege?.toString()
        ?: this.date?.toString()
        ?: this.bytes?.toString()!!
}