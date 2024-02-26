
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.oidc.AuthenticationRequestParameters
import at.asitplus.wallet.lib.oidvci.decodeFromUrlQuery
import io.ktor.http.Url
import io.ktor.http.parseQueryString
import io.ktor.util.flattenEntries
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import navigation.AuthenticationQrCodeScannerPage
import navigation.CameraPage
import navigation.ConsentPage
import navigation.CredentialPage
import navigation.HomePage
import navigation.InformationPage
import navigation.LoadingPage
import navigation.LogPage
import navigation.NavigationStack
import navigation.Page
import navigation.PayloadPage
import navigation.QrCodeCredentialScannerPage
import navigation.RefreshCredentialsPage
import navigation.ShowDataPage
import ui.views.AuthenticationConsentPage
import ui.views.AuthenticationConsentView
import ui.views.AuthenticationQrCodeScannerView
import ui.views.LoadDataView
import ui.views.SettingsView
import ui.views.ShowDataView
import ui.views.CameraView
import view.ConsentScreen
import view.CredentialScreen
import view.LoadingScreen
import view.LogScreen
import view.MyCredentialsScreen
import view.PayloadScreen
import view.QrCodeCredentialScannerScreen

//@Composable
//fun navigatorBackup(walletMain: WalletMain) {
//    // Modified from https://github.com/JetBrains/compose-multiplatform/tree/master/examples/imageviewer
//    val navigationStack = rememberSaveable(
//        saver = listSaver<NavigationStack<Page>, Page>(
//            restore = { NavigationStack(*it.toTypedArray()) },
//            save = { it.stack },
//        )
//    ) {
//        NavigationStack(HomePage())
//    }
//
//    globalBack = { navigationStack.back() }
//
//    LaunchedEffect(appLink.value){
//        appLink.value?.let { link ->
//            val parameterIndex = link.indexOfFirst { it == '?' }
//            val pars = parseQueryString(link, startIndex = parameterIndex + 1)
//
//            if (pars.contains("error")) {
//                walletMain.errorService.emit(Exception(pars["error_description"] ?: Resources.UNKNOWN_EXCEPTION))
//                appLink.value = null
//                return@LaunchedEffect
//            }
//
//            val host = walletMain.walletConfig.host
//            if (link.contains("$host/mobile") == true){
//                val params = kotlin.runCatching {
//                    Url(link).parameters.flattenEntries().toMap().decodeFromUrlQuery<AuthenticationRequestParameters>()
//                }
//
//                val requestedClaims = params.getOrNull()?.presentationDefinition?.inputDescriptors
//                    ?.mapNotNull { it.constraints }?.flatMap { it.fields?.toList() ?: listOf() }
//                    ?.flatMap { it.path.toList() }
//                    ?.filter { it != "$.type" }
//                    ?.filter { it != "$.mdoc.doctype" }
//                    ?.map { it.removePrefix("\$.mdoc.") }
//                    ?.map { it.removePrefix("\$.") }
//                    ?: listOf()
//                if (walletMain.subjectCredentialStore.credentialSize.value != 0) {
//                    navigationStack.push(ConsentPage(url = link, claims = requestedClaims, recipientName = "DemoService", recipientLocation = "DemoLocation"))
//                    appLink.value = null
//                    return@LaunchedEffect
//                } else {
//                    walletMain.errorService.emit(Exception("NoCredentialException"))
//                    appLink.value = null
//                    return@LaunchedEffect
//                }
//
//            }
//            if (walletMain.provisioningService.redirectUri?.let { link.contains(it) } == true) {
//                navigationStack.push(LoadingPage())
//                walletMain.scope.launch {
//
//                    try {
//                        walletMain.provisioningService.handleResponse(link)
//                        walletMain.snackbarService.showSnackbar(Resources.SNACKBAR_CREDENTIAL_LOADED_SUCCESSFULLY)
//                        navigationStack.back()
//
//                    } catch (e: Throwable) {
//                        navigationStack.back()
//                        walletMain.errorService.emit(e)
//
//                    }
//                    appLink.value = null
//                }
//                return@LaunchedEffect
//            }
//        }
//    }
//
//
//
//
//    AnimatedContent(targetState = navigationStack.lastWithIndex()) { (_, page) ->
//        when (page) {
//            is HomePage -> {
//                HomeScreen(
//                    onAbout = { navigationStack.push(AboutPage()) },
//                    onCredential = { info ->
//                        navigationStack.push(CredentialPage(info))
//                    },
//                    onScanQrCode = { navigationStack.push(CameraPage()) },
//                    onLoginWithIdAustria = {
//                        walletMain.scope.launch {
//                            try {
//                                walletMain.provisioningService.startProvisioning()
//                            } catch (e: Throwable) {
//                                walletMain.errorService.emit(e)
//                            }
//                        }
//                    },
//                    walletMain = walletMain
//                )
//            }
//
//            is AboutPage -> {
//                AboutScreen(
//                    onShowLog = {navigationStack.push(LogPage())},
//                    walletMain)
//            }
//
//            is LogPage -> {
//                LogScreen(walletMain)
//            }
//
//            is CredentialPage -> {
//                CredentialScreen(id = page.info, walletMain)
//            }
//
//            is CameraPage -> {
//                CameraView(
//                    onFoundPayload = { info ->
//                        navigationStack.push(PayloadPage(info))
//                    }
//                )
//            }
//
//            is PayloadPage -> {
//                PayloadScreen(
//                    text = page.info,
//                    onContinueClick = { navigationStack.push(HomePage()) },
//                    walletMain
//                )
//
//            }
//
//            is ConsentPage -> {
//                ConsentScreen(
//                    walletMain = walletMain,
//                    onAccept = {navigationStack.push(HomePage())},
//                    onCancel = {navigationStack.back()},
//                    url = page.url,
//                    recipientName = page.recipientName,
//                    recipientLocation = page.recipientLocation,
//                    claims = page.claims
//                )
//            }
//
//            is LoadingPage -> {
//                LoadingScreen()
//            }
//        }
//    }
//}


private enum class NavigationData(
    val title: String,
    val icon: @Composable () -> Unit,
    val destination: Page,
    val isActive: (Page) -> Boolean
) {
    HOME_SCREEN(
        title = "Meine Daten",
        icon = {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Meine Daten ansehen",
            )
        },
        destination = HomePage(),
        isActive = {
            when (it) {
                is HomePage -> true
                else -> false
            }
        },
    ),
    SHOW_DATA_SCREEN(
        title = "Daten Vorzeigen",
        icon = {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = "Daten Vorzeigen",
            )
        },
        destination = ShowDataPage(),
        isActive = {
            when (it) {
                is ShowDataPage -> true
                else -> false
            }
        },
    ),
    INFORMATION_SCREEN(
        title = "Einstellungen",
        icon = {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Einstellungen",
            )
        },
        destination = InformationPage(),
        isActive = {
            when (it) {
                is InformationPage -> true
                else -> false
            }
        },
    ),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Navigator(walletMain: WalletMain) {
    // TODO("get stage and version")
    val stage = "T"
    val version = "1.0.0 / 2389237"

    // Modified from https://github.com/JetBrains/compose-multiplatform/tree/master/examples/imageviewer
    val navigationStack = rememberSaveable(
        saver = listSaver<NavigationStack<Page>, Page>(
            restore = { NavigationStack(*it.toTypedArray()) },
            save = { it.stack },
        )
    ) {
        NavigationStack(HomePage())
    }

    LaunchedEffect(appLink.value) {
        appLink.value?.let { link ->
            val parameterIndex = link.indexOfFirst { it == '?' }
            val pars = parseQueryString(link, startIndex = parameterIndex + 1)

            if (pars.contains("error")) {
                walletMain.errorService.emit(
                    Exception(
                        pars["error_description"] ?: Resources.UNKNOWN_EXCEPTION
                    )
                )
                appLink.value = null
                return@LaunchedEffect
            }

            val host = walletMain.walletConfig.host
            if (link.contains("$host/mobile") == true) {
                val params = kotlin.runCatching {
                    Url(link).parameters.flattenEntries().toMap()
                        .decodeFromUrlQuery<AuthenticationRequestParameters>()
                }

                val requestedClaims = params.getOrNull()?.presentationDefinition?.inputDescriptors
                    ?.mapNotNull { it.constraints }?.flatMap { it.fields?.toList() ?: listOf() }
                    ?.flatMap { it.path.toList() }
                    ?.filter { it != "$.type" }
                    ?.filter { it != "$.mdoc.doctype" }
                    ?.map { it.removePrefix("\$.mdoc.") }
                    ?.map { it.removePrefix("\$.") }
                    ?: listOf()
                if (walletMain.subjectCredentialStore.observeCredentialSize().first() != 0) {
                    navigationStack.push(
                        ConsentPage(
                            url = link,
                            claims = requestedClaims,
                            recipientName = "DemoService",
                            recipientLocation = "DemoLocation",
                        )
                    )
                    appLink.value = null
                    return@LaunchedEffect
                } else {
                    walletMain.errorService.emit(Exception("NoCredentialException"))
                    appLink.value = null
                    return@LaunchedEffect
                }
            }

            if (walletMain.provisioningService.redirectUri?.let { link.contains(it) } == true) {
                navigationStack.push(LoadingPage())
                walletMain.scope.launch {
                    try {
                        walletMain.provisioningService.handleResponse(link)
                        walletMain.snackbarService.showSnackbar(Resources.SNACKBAR_CREDENTIAL_LOADED_SUCCESSFULLY)
                        globalBack()
                    } catch (e: Throwable) {
                        globalBack()
                        walletMain.errorService.emit(e)
                    }
                    appLink.value = null
                }
                return@LaunchedEffect
            }
        }
    }

    val navigationData = { page: Page ->
        when (page) {
            is HomePage -> {
                NavigationData.HOME_SCREEN
            }

            is ShowDataPage -> {
                NavigationData.SHOW_DATA_SCREEN
            }

            is InformationPage -> {
                NavigationData.INFORMATION_SCREEN
            }

            else -> null
        }
    }

    globalBack = { navigationStack.back() }

    val startProvisioning: () -> Unit = {
        walletMain.scope.launch {
            try {
                walletMain.provisioningService.startProvisioning()
            } catch (e: Exception) {
                walletMain.errorService.emit(e)
            }
        }
    }

    val launchQrCodeScannerForDataProvisioning: () -> Unit = {
        navigationStack.push(
            QrCodeCredentialScannerPage()
        )
    }

    Scaffold(
        bottomBar = {
            val (_, page) = navigationStack.lastWithIndex()
            val pageNavigationData = navigationData(page)
            if (pageNavigationData != null) {
                NavigationBar {
                    for (route in listOf(
                        NavigationData.HOME_SCREEN,
                        NavigationData.SHOW_DATA_SCREEN,
                        NavigationData.INFORMATION_SCREEN,
                    )) {
                        NavigationBarItem(
                            icon = route.icon,
                            label = {
                                Text(route.title)
                            },
                            onClick = {
                                navigationStack.push(route.destination)
                            },
                            selected = route.isActive(page)
                        )
                    }
                }
            }
        },
    ) { scaffoldPadding ->
        Box(modifier = Modifier.padding(scaffoldPadding)) {
            AnimatedContent(targetState = navigationStack.lastWithIndex()) { (_, page) ->
                when (page) {
                    is HomePage -> {
                        val storeContainerState by walletMain.subjectCredentialStore.observeStoreContainer()
                            .collectAsState(null)

                        storeContainerState?.let { storeContainer ->
                            MyCredentialsScreen(
                                credentials = storeContainer.credentials,
                                refreshCredentials = {
                                    navigationStack.push(RefreshCredentialsPage())
                                },
                                startProvisioning = startProvisioning,
                                startProvisioningFromQrCode = launchQrCodeScannerForDataProvisioning,
                                decodeImage = walletMain.platformAdapter::decodeImage,
                            )
                        }

//                        HomeScreen(
//                            onAbout = { navigationStack.push(AboutPage()) },
//                            onCredential = { info ->
//                                navigationStack.push(CredentialPage(info))
//                            },
//                            onScanQrCode = { navigationStack.push(CameraPage()) },
//                            onLoginWithIdAustria = {
//                                CoroutineScope(Dispatchers.Default).launch {
//                                    try {
//                                        walletMain.provisioningService.startProvisioning()
//                                    } catch (e: Exception) {
//                                        walletMain.errorService.emit(e)
//                                    }
//                                }
//                            },
//                            walletMain = walletMain
//                        )
                    }

                    is QrCodeCredentialScannerPage -> {
                        QrCodeCredentialScannerScreen(
                            navigateUp = globalBack,
                            onPayloadFound = { payload ->
                                TODO("Missing Implementation")
                            }
                        )
                    }

                    is RefreshCredentialsPage -> {
                        LoadDataView(
                            navigateUp = globalBack,
                            loadData = startProvisioning,
                            onLoadDataFromQrCode = launchQrCodeScannerForDataProvisioning,
                        )
                    }

                    is InformationPage -> {
                        var credentialRepresentation by remember {
                            runBlocking {
                                mutableStateOf(walletMain.walletConfig.credentialRepresentation.first())
                            }
                        }
                        var host by rememberSaveable {
                            runBlocking {
                                mutableStateOf(walletMain.walletConfig.host.first())
                            }
                        }
                        var isSaveEnabled by rememberSaveable {
                            mutableStateOf(false)
                        }

                        SettingsView(
                            host = host,
                            onChangeHost = {
                                host = it
                            },
                            credentialRepresentation = credentialRepresentation,
                            onChangeCredentialRepresentation = {
                                credentialRepresentation = it
                            },
                            isSaveEnabled = isSaveEnabled,
                            onChangeIsSaveEnabled = {
                                isSaveEnabled = it
                            },
                            onClickSaveConfiguration = {
                                walletMain.walletConfig.set(
                                    host = host,
                                    credentialRepresentation = credentialRepresentation,
                                )
                            },
                            stage = stage,
                            version = version,
                            onClickFAQs = {},
                            onClickDataProtectionPolicy = {},
                            onClickLicenses = {},
                            onClickShareLogFile = {
                                navigationStack.push(LogPage())
                            },
                            onClickResetApp = {
                                runBlocking { walletMain.resetApp() }
                                walletMain.snackbarService.showSnackbar(Resources.SNACKBAR_RESET_APP_SUCCESSFULLY)
                            },
                        )
                    }

                    is LogPage -> {
                        val logArray = try {
                            walletMain.getLog()
                        } catch (e: Throwable) {
                            walletMain.errorService.emit(e)
                            listOf()
                        }

                        LogScreen(
                            logArray = logArray,
                            navigateUp = globalBack,
                            shareLog = {
                                walletMain.scope.launch {
                                    walletMain.platformAdapter.shareLog()
                                }
                            }
                        )
                    }

//                    is AboutPage -> {
//                        AboutScreen(
//                            onShowLog = {},
//                            walletMain = walletMain,
//                            navigateUp = globalBack,
//                        )
//                    }

                    is ConsentPage -> {
                        ConsentScreen(
                            navigateUp = globalBack,
                            onAccept = {
                                walletMain.scope.launch {
                                    try {
                                        walletMain.presentationService.startSiop(page.url)
                                    } catch (e: Throwable) {
                                        walletMain.errorService.emit(e)
                                    }
                                    navigationStack.push(HomePage())
                                }
                            },
                            onCancel = globalBack,
                            recipientName = page.recipientName,
                            recipientLocation = page.recipientLocation,
                            claims = page.claims,
                        )
                    }

                    is CredentialPage -> {
                        CredentialScreen(
                            id = page.info,
                            navigateUp = globalBack,
                            walletMain,
                        )
                    }

                    is CameraPage -> {
                        CameraView(
                            onFoundPayload = { info ->
                                navigationStack.push(PayloadPage(info))
                            }
                        )
                    }

                    is PayloadPage -> {
                        PayloadScreen(
                            text = page.info,
                            onContinueClick = { navigationStack.push(HomePage()) },
                            walletMain = walletMain,
                        )
                    }

                    is LoadingPage -> {
                        LoadingScreen()
                    }


                    is ShowDataPage -> {
                        ShowDataView(
                            navigateToAuthenticationAtSp = {
                                navigationStack.push(AuthenticationQrCodeScannerPage())
//                                navigationStack.push(
//                                    AuthenticationConsentPage(
//                                        spName = "Post-Schalter#3",
//                                        spLocation = "St. Peter Hauptstraße\n8010, Graz",
//                                        requestedAttributes = mapOf(
//                                            PersonalDataCategory.IdentityData to listOf(
//                                                AttributeAvailability(
//                                                    attributeName = "Vorname",
//                                                    isAvailable = false,
//                                                ),
//                                                AttributeAvailability(
//                                                    attributeName = "Nachname",
//                                                    isAvailable = false,
//                                                ),
//                                                AttributeAvailability(
//                                                    attributeName = "Aktuelles Foto aus zentralem Identitätsdokumentenregister",
//                                                    isAvailable = false,
//                                                ),
//                                            ),
//                                            PersonalDataCategory.ResidenceData to listOf(
//                                                AttributeAvailability(
//                                                    attributeName = "Straße",
//                                                    isAvailable = false,
//                                                ),
//                                                AttributeAvailability(
//                                                    attributeName = "Hausnummer",
//                                                    isAvailable = false,
//                                                ),
//                                                AttributeAvailability(
//                                                    attributeName = "Postleitzahl",
//                                                    isAvailable = true,
//                                                ),
//                                                AttributeAvailability(
//                                                    attributeName = "Ort",
//                                                    isAvailable = true,
//                                                ),
//                                            ),
//                                        ).toList()
//                                    )
//                                )
                            },
                            navigateToShowDataToExecutive = {
                                walletMain.snackbarService.showSnackbar("Incomplete Implementation")
                            },
                            navigateToShowDataToOtherCitizen = {
                                walletMain.snackbarService.showSnackbar("Incomplete Implementation")
                            },
                        )
                    }

                    is AuthenticationQrCodeScannerPage -> {
                        AuthenticationQrCodeScannerView(
                            navigateUp = globalBack,
                            onPayloadFound = { payload ->
                                globalBack()
                                // replace with opening consent page by link
                                TODO()
//                                navigationStack.push(
//                                    AuthenticationSPInfoPage(
//                                        spName = "name1",
//                                        spLocation = "location1",
//                                    )
//                                )
                            },
                        )
                    }
//
//                    is AuthenticationSPInfoPage -> {
//                        AuthenticationSPInfoView(
//                            navigateUp = globalBack,
//                            cancelAuthentication = globalBack,
//                            authenticateAtSp = {},
//                            spName = page.spName,
//                            spLocation = page.spLocation,
//                            spImage = null,
//                        )
//                    }

                    is AuthenticationConsentPage -> {
                        val bottomSheetState = rememberModalBottomSheetState()
                        var showBottomSheet by remember { mutableStateOf(false) }

                        AuthenticationConsentView(
                            navigateUp = globalBack,
                            cancelAuthentication = globalBack,
                            consentToDataTransmission = {
                                showBottomSheet = true
                            },
                            loadMissingData = {
                                globalBack()

                                navigationStack.push(AuthenticationConsentPage(
                                    spName = page.spName,
                                    spLocation = page.spLocation,
                                    requestedAttributes = page.requestedAttributes.map {
                                        it.copy(second = it.second.map {
                                            it.copy(isAvailable = true)
                                        })
                                    }
                                ))
                            },
                            requestedAttributes = page.requestedAttributes,
                            spName = page.spName,
                            spLocation = page.spLocation,
                            spImage = null,
                            bottomSheetState = bottomSheetState,
                            showBottomSheet = showBottomSheet,
                            onBottomSheetDismissRequest = {
                                showBottomSheet = !showBottomSheet
                            }
                        )
                    }
                }
            }
        }
    }
}