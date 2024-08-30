package ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import at.asitplus.signum.indispensable.cosef.io.ByteStringWrapper
import at.asitplus.wallet.app.common.CryptoServiceAuthorizationContext
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.agent.PresentationException
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.cbor.CoseService
import at.asitplus.wallet.lib.cbor.DefaultCoseService
import at.asitplus.wallet.lib.iso.DeviceAuth
import at.asitplus.wallet.lib.iso.DeviceSigned
import at.asitplus.wallet.lib.iso.Document
import at.asitplus.wallet.lib.iso.IssuerSigned
import at.asitplus.wallet.lib.iso.IssuerSignedItem
import at.asitplus.wallet.lib.iso.IssuerSignedList
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.heading_label_select_requested_data
import composewalletapp.shared.generated.resources.section_heading_selected
import composewalletapp.shared.generated.resources.section_heading_available
import composewalletapp.shared.generated.resources.section_heading_requested
import composewalletapp.shared.generated.resources.section_heading_response_sent
import composewalletapp.shared.generated.resources.section_heading_sending_response
import data.bletransfer.Holder
import data.bletransfer.holder.RequestedDocument
import data.bletransfer.verifier.DocumentAttributes
import data.bletransfer.verifier.ValueType
import data.storage.StoreContainer
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.stringResource
import ui.composables.buttons.NavigateUpButton

@Composable
fun HandleRequestedDataScreen(holder: Holder, navigateUp: () -> Unit, walletMain: WalletMain) {
    val requestedAttributes = holder.getAttributes()

    HandleRequestedDataView(walletMain, holder, requestedAttributes) {
        holder.disconnect()
        navigateUp()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandleRequestedDataView(
    walletMain: WalletMain,
    holder: Holder,
    requestedAttributes: List<RequestedDocument>,
    navigateUp: () -> Unit
) {
    val storeContainer = walletMain.subjectCredentialStore.observeStoreContainer()
    val storeContainerState by storeContainer.collectAsState(null)
    var view by remember { mutableStateOf(HandleRequestedDataView.SELECTION) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(Res.string.heading_label_select_requested_data),
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

            when (view) {
                HandleRequestedDataView.SELECTION -> {
                    selectRequestedDataView(
                        walletMain = walletMain,
                        holder = holder,
                        requestedAttributes = requestedAttributes,
                        storeContainerState = storeContainerState,
                        changeToLoading = { view = HandleRequestedDataView.LOADING },
                        changeToSent = { view = HandleRequestedDataView.SENT }
                    )
                }

                HandleRequestedDataView.LOADING -> {
                    sendingRequestedDataView()
                }

                HandleRequestedDataView.SENT -> {
                    sentRequestedDataView()
                }
            }
        }
    }
}

fun getSelectedCredentials(
    walletMain: WalletMain,
    credentials: List<SubjectCredentialStore.StoreEntry>?,
    requestedAttributes: List<RequestedDocument>
): MutableList<Document> {
    val selectedCredentials: MutableList<Document> = mutableListOf()

    requestedAttributes.forEach { reqDocument ->
        val ret = createDocument(walletMain, reqDocument, credentials)
        selectedCredentials.addNull(ret)
    }
    return selectedCredentials
}

fun createDocument(
    walletMain: WalletMain,
    reqDocument: RequestedDocument,
    credentials: List<SubjectCredentialStore.StoreEntry>?
): Document? {

    reqDocument.nameSpaces.forEach { nameSpace ->
        val correctCredentials = credentials?.filter { cred ->
            when (cred) {
                is SubjectCredentialStore.StoreEntry.Iso -> {
                    cred.issuerSigned.namespaces?.any { namespace ->
                        nameSpace.nameSpace == namespace.key
                    } ?: false
                }

                else -> false
            }
        }
        // Here could be more than one credentials if the app has 2 credentials with the same nameSpace.
        correctCredentials?.get(0)?.let { cCred ->
            when (cCred) {
                is SubjectCredentialStore.StoreEntry.Iso -> {
                    cCred.issuerSigned.namespaces?.get(nameSpace.nameSpace)?.let { namespace ->

                        val newnamespaces: List<ByteStringWrapper<IssuerSignedItem>> =
                            namespace.entries.filter { entry ->
                                nameSpace.trueAttributes.contains(DocumentAttributes.fromValue(entry.value.elementIdentifier))
                            }


                        val imageAttributes: List<String> = DocumentAttributes.entries
                            .filter { it.type == ValueType.IMAGE }
                            .map { it.value }

                        if (imageAttributes.isNotEmpty()) {
                            val map = mapOf(
                                nameSpace.nameSpace to IssuerSignedList(newnamespaces)
                            )
                            return runBlocking {
                                val coseService: CoseService =
                                    DefaultCoseService(walletMain.cryptoService)
                                val deviceSignature = coseService.createSignedCose(
                                    addKeyId = false
                                ).getOrElse {
                                    Napier.w("Could not create DeviceAuth for presentation", it)
                                    throw PresentationException(it)
                                }


                                val issuerSigned = IssuerSigned(map, cCred.issuerSigned.issuerAuth)
                                Document(
                                    reqDocument.docType, issuerSigned,
                                    DeviceSigned(
                                        namespaces = byteArrayOf(),
                                        deviceAuth = DeviceAuth(
                                            deviceSignature = deviceSignature
                                        )
                                    )
                                )

                            }
                        }
                    }
                }

                else -> {}
            }
        }
    }
    return null
}


@Composable
fun selectRequestedDataView(
    walletMain: WalletMain,
    holder: Holder,
    requestedAttributes: List<RequestedDocument>,
    storeContainerState: StoreContainer?,
    changeToLoading: () -> Unit,
    changeToSent: () -> Unit
) {
    val uncheckedAttributes = remember { mutableStateListOf<DocumentAttributes>() }
    val credentials = storeContainerState?.credentials
    val authorizationContext = presentationAuthorizationContext()
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = {
                        Icon(
                            painter = rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowForward),
                            contentDescription = "Send Selected Data",
                        )
                    },
                    label = {},
                    onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            walletMain.cryptoService.useAuthorizationContext(authorizationContext) {
                                changeToLoading()
                                val documentsToSend = getSelectedCredentials(
                                    walletMain,
                                    credentials?.map { it.second },
                                    requestedAttributes
                                )
                                holder.send(documentsToSend, changeToSent)
                            }
                        }
                    },
                    selected = false,
                )
            }
        },
    ) {
        Column(
            modifier = Modifier.padding(end = 16.dp, start = 16.dp, bottom = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            requestedAttributes.forEach { requestedDocument: RequestedDocument ->
                Text(text = "docType: ${requestedDocument.docType}")
                requestedDocument.nameSpaces.forEach { nameSpace: RequestedDocument.NameSpace ->
                    Text(text = "nameSpace: ${nameSpace.nameSpace}")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(Res.string.section_heading_requested),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(2f)
                        )
                        Text(
                            text = stringResource(Res.string.section_heading_available),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stringResource(Res.string.section_heading_selected),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    nameSpace.attributes.forEach { item: DocumentAttributes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(item.displayName),
                                modifier = Modifier.weight(2f)
                            )

                            val isAvailableChecked = credentialsContainAttribute(
                                storeContainerState,
                                item
                            )
                            Checkbox(
                                checked = isAvailableChecked,
                                onCheckedChange = {},
                                modifier = Modifier.weight(1f).size(30.dp)
                            )

                            val isSendingChecked =
                                isAvailableChecked && !uncheckedAttributes.contains(
                                    item
                                )
                            Checkbox(
                                checked = isSendingChecked,
                                modifier = Modifier.weight(1f).size(30.dp),
                                onCheckedChange = {
                                    if (isAvailableChecked) {
                                        uncheckedAttributes.toggleElement(item)
                                    }
                                })
                            nameSpace.attributesMap[item] = isSendingChecked
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun sendingRequestedDataView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.section_heading_sending_response),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun sentRequestedDataView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = rememberVectorPainter(Icons.Default.Check),
                contentDescription = ""
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.section_heading_response_sent),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun credentialsContainAttribute(
    storeContainerState: StoreContainer?,
    attribute: DocumentAttributes
): Boolean {
    return storeContainerState?.let { storeContainer ->
        storeContainer.credentials.any { cred ->
            val credsecond = cred.second
            when (credsecond) {
                is SubjectCredentialStore.StoreEntry.Iso -> {
                    credsecond.issuerSigned.namespaces?.any { namespace ->
                        namespace.value.entries.any { entry ->
                            entry.value.elementIdentifier == attribute.value
                        }
                    } ?: false
                }

                else -> false
            }
        }
    } ?: false
}

private fun <T> MutableList<T>.toggleElement(element: T) {
    if (contains(element)) {
        remove(element)
    } else {
        add(element)
    }
}

private fun <T> MutableList<T>.addNull(element: T?) {
    element?.let {
        add(element)
    }
}

enum class HandleRequestedDataView {
    SELECTION,
    LOADING,
    SENT,
}

