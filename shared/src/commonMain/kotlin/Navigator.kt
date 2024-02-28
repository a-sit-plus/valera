
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.oidc.AuthenticationRequestParameters
import at.asitplus.wallet.lib.oidvci.decodeFromUrlQuery
import io.github.aakira.napier.Napier
import io.ktor.http.Url
import io.ktor.http.parseQueryString
import io.ktor.util.flattenEntries
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import navigation.AuthenticationConsentPage
import navigation.AuthenticationQrCodeScannerPage
import navigation.AuthenticationSuccessPage
import navigation.HomePage
import navigation.LoadingPage
import navigation.LogPage
import navigation.NavigationStack
import navigation.Page
import navigation.QrCodeCredentialScannerPage
import navigation.RefreshCredentialsPage
import navigation.SettingsPage
import navigation.ShowDataPage
import ui.views.LoadDataScreen
import view.AuthenticationConsentScreen
import view.AuthenticationQrCodeScannerScreen
import view.AuthenticationSuccessScreen
import view.ErrorScreen
import view.LoadingScreen
import view.LogScreen
import view.MyCredentialsScreen
import view.OnboardingWrapper
import view.QrCodeCredentialScannerScreen
import view.SettingsScreen
import view.ShowDataScreen

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
        destination = SettingsPage(),
        isActive = {
            when (it) {
                is SettingsPage -> true
                else -> false
            }
        },
    ),
}

@Composable
fun Navigator(walletMain: WalletMain) {
    // Modified from https://github.com/JetBrains/compose-multiplatform/tree/master/examples/imageviewer
    val mainNavigationStack = rememberSaveable(
        saver = listSaver<NavigationStack<Page>, Page>(
            restore = { NavigationStack(*it.toTypedArray()) },
            save = { it.stack },
        )
    ) {
        NavigationStack(HomePage())
    }

    LaunchedEffect(appLink.value) {
        Napier.d {
            "app link changed to ${appLink.value}"
        }
        appLink.value?.let { link ->
            // resetting error service so that the intent can be displayed as intended
            walletMain.errorService.reset()

            Napier.d {
                "new app link: ${link}"
            }
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

            val host = walletMain.walletConfig.host.first()
            if (link.contains("$host/mobile")) {
                Napier.d {
                    "authentication request"
                }
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

                mainNavigationStack.push(
                    AuthenticationConsentPage(
                        url = link,
                        claims = requestedClaims,
                        recipientName = "DemoService",
                        recipientLocation = "DemoLocation",
                    )
                )
                appLink.value = null
                return@LaunchedEffect
            }

            if (walletMain.provisioningService.redirectUri?.let { link.contains(it) } == true) {
                mainNavigationStack.push(LoadingPage())
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

    globalBack = { mainNavigationStack.back() }

    if (walletMain.errorService.showError.value == false) {
        OnboardingWrapper(
            walletMain = walletMain,
        ) {
            MainNavigator(
                navigationStack = mainNavigationStack,
                navigateUp = globalBack,
                walletMain = walletMain,
            )
        }
    } else {
        ErrorScreen(walletMain)
    }
}

@Composable
fun MainNavigator(
    navigationStack: NavigationStack<Page>,
    navigateUp: () -> Unit,
    walletMain: WalletMain,
) {
//    // Modified from https://github.com/JetBrains/compose-multiplatform/tree/master/examples/imageviewer
//    val navigationStack = rememberSaveable(
//        saver = listSaver<NavigationStack<Page>, Page>(
//            restore = { NavigationStack(*it.toTypedArray()) },
//            save = { it.stack },
//        )
//    ) {
//        NavigationStack(HomePage())
//    }

    Scaffold(
        bottomBar = {
            val (_, page) = navigationStack.lastWithIndex()
            val pageNavigationData = when (page) {
                is HomePage -> {
                    NavigationData.HOME_SCREEN
                }

                is ShowDataPage -> {
                    NavigationData.SHOW_DATA_SCREEN
                }

                is SettingsPage -> {
                    NavigationData.INFORMATION_SCREEN
                }

                else -> null
            }

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
                        MyCredentialsScreen(
                            navigateToRefreshCredentialsPage = {
                                navigationStack.push(RefreshCredentialsPage())
                            },
                            navigateToQrCodeCredentialProvisioningPage = {
                                navigationStack.push(
                                    QrCodeCredentialScannerPage()
                                )
                            },
                            walletMain = walletMain,
                        )
                    }

                    is QrCodeCredentialScannerPage -> {
                        QrCodeCredentialScannerScreen(
                            navigateUp = navigateUp,
                            walletMain = walletMain,
                        )
                    }

                    is RefreshCredentialsPage -> {
                        LoadDataScreen(
                            navigateUp = navigateUp,
                            navigateToQrCodeCredentialProvisioningPage = {
                                navigationStack.push(
                                    QrCodeCredentialScannerPage()
                                )
                            },
                            walletMain = walletMain,
                        )
                    }

                    is SettingsPage -> {
                        SettingsScreen(
                            navigateToLogPage = {
                                navigationStack.push(LogPage())
                            },
                            onClickResetApp = {
                                runBlocking { walletMain.resetApp() }
                                navigationStack.reset()
                                walletMain.snackbarService.showSnackbar(Resources.SNACKBAR_RESET_APP_SUCCESSFULLY)
                            },
                            walletMain = walletMain,
                        )
                    }

                    is LogPage -> {
                        LogScreen(
                            navigateUp = navigateUp,
                            walletMain = walletMain,
                        )
                    }

                    is LoadingPage -> {
                        LoadingScreen()
                    }


                    is ShowDataPage -> {
                        ShowDataScreen(
                            navigateToAuthenticationStartPage = {
                                navigationStack.push(AuthenticationQrCodeScannerPage())
//                                navigationStack.push(
//                                    AuthenticationConsentPage(
//                                        url = "TODO()",
//                                        claims = listOf(
//                                            IdAustriaAttribute.FirstName,
//                                            IdAustriaAttribute.LastName,
//                                            IdAustriaAttribute.AgeAtLeast18,
//                                        ).map { it.attributeName },
//                                        recipientName = "Post-Schalter#3",
//                                        recipientLocation = "St. Peter Hauptstraße\n8010, Graz",
//                                    )
//                                )
                            },
                            onClickShowDataToExecutive = {
                                walletMain.snackbarService.showSnackbar("Incomplete Implementation")
                            },
                            onClickShowDataToOtherCitizen = {
                                walletMain.snackbarService.showSnackbar("Incomplete Implementation")
                            },
                        )
                    }

                    is AuthenticationQrCodeScannerPage -> {
                        AuthenticationQrCodeScannerScreen(
                            navigateUp = navigateUp,
                            walletMain = walletMain,
                        )
                    }

                    is AuthenticationConsentPage -> {
                        AuthenticationConsentScreen(
                            spName = page.recipientName,
                            spLocation = page.recipientLocation,
                            spImage = null,
                            claims = page.claims,
                            url = page.url,
                            navigateUp = navigateUp,
                            navigateToRefreshCredentialsPage = {
                                navigationStack.push(
                                    RefreshCredentialsPage()
                                )
                            },
                            navigateToAuthenticationSuccessPage = {
                                navigationStack.push(
                                    AuthenticationSuccessPage()
                                )
                            },
                            walletMain = walletMain,
                        )
                    }

                    is AuthenticationSuccessPage -> {
                        AuthenticationSuccessScreen(
                            navigateUp = navigateUp,
                        )
                    }
                }
            }
        }
    }
}