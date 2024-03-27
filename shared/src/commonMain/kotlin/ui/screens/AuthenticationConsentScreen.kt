package ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.data.dif.PresentationDefinition
import at.asitplus.wallet.lib.oidc.AuthenticationRequestParameters
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.error_authentication_at_sp_failed
import data.CredentialExtractor
import data.attributeTranslation
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import ui.composables.AttributeAvailability
import ui.composables.PersonalDataCategory
import ui.composables.attributeCategorizationOrder
import ui.views.AuthenticationConsentView

@Composable
fun AuthenticationConsentScreen(
    spName: String,
    spLocation: String,
    spImage: ImageBitmap?,
    authenticationRequestParameters: AuthenticationRequestParameters,
    fromQrCodeScanner: Boolean,
    navigateUp: () -> Unit,
    navigateToRefreshCredentialsPage: () -> Unit,
    navigateToAuthenticationSuccessPage: () -> Unit,
    walletMain: WalletMain,
) {
    val storeContainerState by walletMain.subjectCredentialStore.observeStoreContainer()
        .collectAsState(null)

    storeContainerState?.let { storeContainer ->
        val credentialExtractor = CredentialExtractor(storeContainer.credentials)

        AuthenticationConsentViewStateHolder(
            spName = spName,
            spLocation = spLocation,
            spImage = spImage,
            authenticationRequestParameters = authenticationRequestParameters,
            credentialExtractor = credentialExtractor,
            fromQrCodeScanner = fromQrCodeScanner,
            navigateUp = navigateUp,
            navigateToRefreshCredentialsPage = navigateToRefreshCredentialsPage,
            navigateToAuthenticationSuccessPage = navigateToAuthenticationSuccessPage,
            walletMain = walletMain,
        )
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun AuthenticationConsentViewStateHolder(
    spName: String,
    spLocation: String,
    spImage: ImageBitmap?,
    authenticationRequestParameters: AuthenticationRequestParameters,
    credentialExtractor: CredentialExtractor,
    fromQrCodeScanner: Boolean,
    navigateUp: () -> Unit,
    navigateToRefreshCredentialsPage: () -> Unit,
    navigateToAuthenticationSuccessPage: () -> Unit,
    walletMain: WalletMain,
) {
    val attributeCategorization = attributeCategorizationOrder.associateWith {
        it.attributes.values.flatten()
    }

    val categorizedClaims = attributeCategorization.toList().flatMap {
        it.second
    }

    val claims = authenticationRequestParameters.presentationDefinition?.claims ?: listOf()
    val attributeCategorizationWithOthers = attributeCategorization + Pair(
        PersonalDataCategory.OtherData, claims.filter {
            categorizedClaims.contains(it) == false
        }
    )

    val requestedAttributesLocalized =
        attributeCategorizationWithOthers.toList().map { attributeCategory ->
            Pair(
                attributeCategory.first,
                attributeCategory.second.mapNotNull { claim ->
                    if (claims.contains(claim) == false) null else AttributeAvailability(
                        // also supports claims that are not supported yet (for example claims that may be added later on before the wallet is updated)
                        attributeName = claim.attributeTranslation?.let { stringResource(it) } ?: claim,
                        isAvailable = credentialExtractor.containsAttribute(claim),
                    )
                }
            )
        }.filter { it.second.isNotEmpty() }

    var showBiometry by rememberSaveable { mutableStateOf(false) }

    AuthenticationConsentView(
        spName = spName,
        spLocation = spLocation,
        spImage = spImage,
        requestedAttributes = requestedAttributesLocalized,
        navigateUp = navigateUp,
        cancelAuthentication = navigateUp,
        loadMissingData = navigateToRefreshCredentialsPage,
        consentToDataTransmission = {
            showBiometry = true
        },
        showBiometry = showBiometry,
        onBiometrySuccess = {
            showBiometry = false
            walletMain.scope.launch {
                try {
                    walletMain.presentationService.startSiop(
                        authenticationRequestParameters,
                        fromQrCodeScanner
                    )
                    navigateUp()
                    navigateToAuthenticationSuccessPage()
                } catch (e: Throwable) {
                    walletMain.errorService.emit(e)
                    walletMain.snackbarService.showSnackbar(getString(Res.string.error_authentication_at_sp_failed))
                }
            }
        },
        onBiometryDismissed = { biometryPromptDismissResult ->
            walletMain.snackbarService.showSnackbar(biometryPromptDismissResult.errorString)
            showBiometry = false
        },
    )
}

private val PresentationDefinition.claims: List<String>
    get() = this.inputDescriptors
        .mapNotNull { it.constraints }.flatMap { it.fields?.toList() ?: listOf() }
        .flatMap { it.path.toList() }
        .filter { it != "$.type" }
        .filter { it != "$.mdoc.doctype" }
        .filter { it != "$.mdoc.namespace" }
        .map { it.removePrefix("\$.mdoc.") }
        .map { it.removePrefix("\$.") }