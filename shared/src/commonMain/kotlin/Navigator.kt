import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import navigation.AboutPage
import navigation.CameraPage
import navigation.ConsentPage
import navigation.CredentialPage
import navigation.HomePage
import navigation.LoadingPage
import navigation.NavigationStack
import navigation.Page
import navigation.PayloadPage
import ui.composables.AttributeAvailability
import ui.composables.PersonalDataCategory
import ui.views.AuthenticationConsentPage
import ui.views.AuthenticationConsentView
import ui.views.AuthenticationQrCodeScannerPage
import ui.views.AuthenticationQrCodeScannerView
import ui.views.AuthenticationSPInfoPage
import ui.views.AuthenticationSPInfoView
import ui.views.InformationPage
import ui.views.InformationView
import ui.views.ShowDataPage
import ui.views.ShowDataView
import view.AboutScreen
import view.CameraView
import view.CredentialScreen
import view.HomeScreen
import view.LoadingScreen
import view.MyDataScreen
import view.PayloadScreen

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



private enum class Route(
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
            when(it) {
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
            when(it) {
                is ShowDataPage -> true
                else -> false
            }
        },
    ),
    INFORMATION_SCREEN(
        title = "Informationen",
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Weitere Information",
            )
        },
        destination = InformationPage(),
        isActive = {
            when(it) {
                is InformationPage -> true
                else -> false
            }
        },
    ),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun navigator(walletMain: WalletMain) {
    // Modified from https://github.com/JetBrains/compose-multiplatform/tree/master/examples/imageviewer
    val navigationStack = rememberSaveable(
        saver = listSaver<NavigationStack<Page>, Page>(
            restore = { NavigationStack(*it.toTypedArray()) },
            save = { it.stack },
        )
    ) {
        NavigationStack(HomePage())
    }

    LaunchedEffect(appLink.value){
        appLink.value?.let { link ->
            val parameterIndex = link.indexOfFirst { it == '?' }
            val pars = parseQueryString(link, startIndex = parameterIndex + 1)

            if (pars.contains("error")) {
                walletMain.errorService.emit(Exception(pars["error_description"] ?: Resources.UNKNOWN_EXCEPTION))
                appLink.value = null
                return@LaunchedEffect
            }

            val host = walletMain.walletConfig.host
            if (link.contains("$host/mobile") == true){
                val params = kotlin.runCatching {
                    Url(link).parameters.flattenEntries().toMap().decodeFromUrlQuery<AuthenticationRequestParameters>()
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
                    navigationStack.push(ConsentPage(url = link, claims = requestedClaims, recipientName = "DemoService", recipientLocation = "DemoLocation"))
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
                        navigationStack.back()

                    } catch (e: Throwable) {
                        navigationStack.back()
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
                Route.HOME_SCREEN
            }

            is ShowDataPage -> {
                Route.SHOW_DATA_SCREEN
            }

            is InformationPage -> {
                Route.INFORMATION_SCREEN
            }

            else -> null
        }
    }

    globalBack = { navigationStack.back() }

    Scaffold(
        bottomBar = {
            val (_, page) = navigationStack.lastWithIndex()
            val pageNavigationData = navigationData(page)
            if (pageNavigationData != null) {
                NavigationBar {
                    for (route in listOf(
                        Route.HOME_SCREEN,
                        Route.SHOW_DATA_SCREEN,
                        Route.INFORMATION_SCREEN,
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
                        MyDataScreen(
                            walletMain = walletMain,
                            refreshCredentials = {
                                CoroutineScope(Dispatchers.Default).launch {
                                    try {
                                        walletMain.provisioningService.startProvisioning()
                                    } catch (e: Exception) {
                                        walletMain.errorService.emit(e)
                                    }
                                }
                            },
                        )
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

                    is AboutPage -> {
                        AboutScreen(
                            onShowLog = {},
                            walletMain = walletMain,
                            navigateUp = {},
                        )
                    }

                    is CredentialPage -> {
                        CredentialScreen(id = page.info, walletMain)
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
                            walletMain
                        )
                    }

                    is LoadingPage -> {
                        LoadingScreen()
                    }


                    is ShowDataPage -> {
                        ShowDataView(
                            navigateToAuthenticationAtSp = {
//                                navigationStack.push(AuthenticationQrCodeScannerPage())
                                navigationStack.push(
                                    AuthenticationConsentPage(
                                        spName = "Post-Schalter#3",
                                        spLocation = "St. Peter Hauptstraße\n8010, Graz",
                                        requestedAttributes = mapOf(
                                            PersonalDataCategory.IdentityData to listOf(
                                                AttributeAvailability(
                                                    attributeName = "Vorname",
                                                    isAvailable = false,
                                                ),
                                                AttributeAvailability(
                                                    attributeName = "Nachname",
                                                    isAvailable = false,
                                                ),
                                                AttributeAvailability(
                                                    attributeName = "Aktuelles Foto aus zentralem Identitätsdokumentenregister",
                                                    isAvailable = false,
                                                ),
                                            ),
                                            PersonalDataCategory.ResidencyData to listOf(
                                                AttributeAvailability(
                                                    attributeName = "Straße",
                                                    isAvailable = false,
                                                ),
                                                AttributeAvailability(
                                                    attributeName = "Hausnummer",
                                                    isAvailable = false,
                                                ),
                                                AttributeAvailability(
                                                    attributeName = "Postleitzahl",
                                                    isAvailable = true,
                                                ),
                                                AttributeAvailability(
                                                    attributeName = "Ort",
                                                    isAvailable = true,
                                                ),
                                            ),
                                        ).toList()
                                    )
                                )
                            },
                            navigateToShowDataToExecutive = {},
                            navigateToShowDataToOtherCitizen = {},
                        )
                    }

                    is InformationPage -> {
                        InformationView(
                            stage = "T",
                            version = "1.0.0 / 2389237",
                            onClickFAQs = {},
                            onClickDataProtectionPolicy = {},
                            onClickLicenses = {},
                            onClickShareLogFile = {},
                            onClickResetApp = {
                                runBlocking { walletMain.resetApp() }
                                walletMain.snackbarService.showSnackbar(Resources.SNACKBAR_RESET_APP_SUCCESSFULLY)
                            },
                        )
                    }

                    is AuthenticationQrCodeScannerPage -> {
                        AuthenticationQrCodeScannerView(
                            navigateUp = globalBack,
                            navigateToConsentScreenWithResult = { name, location ->
                                navigationStack.push(
                                    AuthenticationSPInfoPage(
                                        spName = name,
                                        spLocation = location,
                                    )
                                )
                            },
                        )
                    }

                    is AuthenticationSPInfoPage -> {
                        AuthenticationSPInfoView(
                            navigateUp = globalBack,
                            cancelAuthentication = {},
                            authenticateAtSp = {},
                            spName = page.spName,
                            spLocation = page.spLocation,
                        )
                    }

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