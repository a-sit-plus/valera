import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.oidc.AuthenticationRequestParameters
import at.asitplus.wallet.lib.oidvci.decodeFromUrlQuery
import io.ktor.http.Url
import io.ktor.http.parseQueryString
import io.ktor.util.flattenEntries
import kotlinx.coroutines.launch
import navigation.AboutPage
import navigation.CameraPage
import navigation.ConsentPage
import navigation.CredentialPage
import navigation.HomePage
import navigation.LoadingPage
import navigation.LogPage
import navigation.NavigationStack
import navigation.Page
import navigation.PayloadPage
import view.AboutScreen
import view.CameraView
import view.ConsentScreen
import view.CredentialScreen
import view.HomeScreen
import view.LoadingScreen
import view.LogScreen
import view.PayloadScreen

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

    globalBack = { navigationStack.back() }

    LaunchedEffect(appLink.value){
        appLink.value?.let {val link = it
            val parameterIndex = link.indexOfFirst { it == '?' }
            val pars = parseQueryString(link, startIndex = parameterIndex + 1)

            if (pars.contains("error")) {
                walletMain.errorService.emit(Exception(pars["error_description"].toString()))
                appLink.value = null
            }

            val host = walletMain.walletConfig.host
            if (appLink.value?.contains("$host/mobile") == true){
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
                if (walletMain.subjectCredentialStore.credentialSize.value != 0) {
                    navigationStack.push(ConsentPage(claims = requestedClaims, recipientName = "DemoService", recipientLocation = "DemoLocation"))
                } else {
                    walletMain.errorService.emit(Exception("NoCredentialException"))
                    appLink.value = null
                }

            }
            if (appLink.value?.contains("$host/m1/login/oauth2/code/idaq?code=") == true) {
                navigationStack.push(LoadingPage())
                walletMain.scope.launch {

                    try {
                        walletMain.provisioningService.handleResponse(appLink.value.toString())
                        walletMain.snackbarService.showSnackbar(Resources.SNACKBAR_CREDENTIAL_LOADED_SUCCESSFULLY)
                        navigationStack.back()
                    } catch (e: Throwable) {
                        navigationStack.back()
                        walletMain.errorService.emit(e)
                        appLink.value = null
                    }
                }
            }
        }
    }




    AnimatedContent(targetState = navigationStack.lastWithIndex()) { (_, page) ->
        when (page) {
            is HomePage -> {
                HomeScreen(
                    onAbout = { navigationStack.push(AboutPage()) },
                    onCredential = { info ->
                        navigationStack.push(CredentialPage(info))
                    },
                    onScanQrCode = { navigationStack.push(CameraPage()) },
                    onLoginWithIdAustria = {
                        walletMain.scope.launch {
                            try {
                                walletMain.provisioningService.startProvisioning()
                            } catch (e: Throwable) {
                                walletMain.errorService.emit(e)
                            }
                        }
                    },
                    walletMain = walletMain
                )
            }

            is AboutPage -> {
                AboutScreen(
                    onShowLog = {navigationStack.push(LogPage())},
                    walletMain)
            }

            is LogPage -> {
                LogScreen(walletMain)
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

            is ConsentPage -> {
                ConsentScreen(
                    walletMain = walletMain,
                    onAccept = {navigationStack.push(HomePage())},
                    onCancel = {navigationStack.back()},
                    recipientName = page.recipientName,
                    recipientLocation = page.recipientLocation,
                    claims = page.claims
                )
            }

            is LoadingPage -> {
                LoadingScreen()
            }
        }
    }
}
