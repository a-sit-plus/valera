package ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.data.AttributeIndex
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.dif.PresentationDefinition
import at.asitplus.wallet.lib.data.jsonSerializer
import at.asitplus.wallet.lib.oidc.AuthenticationRequestParameters
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.error_authentication_at_sp_failed
import data.AttributeTranslater
import data.CredentialExtractor
import data.storage.scheme
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import ui.composables.AttributeAvailability
import ui.composables.PersonalDataCategory
import ui.composables.attributeCategorizationOrder
import ui.navigation.RefreshRequirements
import ui.views.AuthenticationConsentView

@Composable
fun AuthenticationConsentScreen(
    spName: String,
    spLocation: String,
    spImage: ImageBitmap?,
    authenticationRequestParameters: AuthenticationRequestParameters,
    fromQrCodeScanner: Boolean,
    navigateUp: () -> Unit,
    navigateToRefreshCredentialsPage: (RefreshRequirements?) -> Unit,
    navigateToAuthenticationSuccessPage: () -> Unit,
    walletMain: WalletMain,
) {
    val storeContainerState by walletMain.subjectCredentialStore.observeStoreContainer()
        .collectAsState(null)

    val requestedCredentialScheme =
        authenticationRequestParameters.presentationDefinition?.inputDescriptors?.first()?.constraints?.fields?.firstOrNull {
            it.path.contains("$.type") or it.path.contains("\$.mdoc.doctype") or it.path.contains("\$.mdoc.namespace")
        }?.filter?.let {
            it.pattern ?: it.const
        }?.let {
            AttributeIndex.resolveAttributeType(it)
                ?: AttributeIndex.resolveSchemaUri(it)
                ?: AttributeIndex.resolveIsoNamespace(it)
        } ?: authenticationRequestParameters.presentationDefinition?.inputDescriptors?.firstOrNull()?.id?.let {
            AttributeIndex.resolveAttributeType(it)
                ?: AttributeIndex.resolveSchemaUri(it)
                ?: AttributeIndex.resolveIsoNamespace(it)
        } ?: throw Exception("Unable to deduce credential scheme")

    val requestedAttributes =
        authenticationRequestParameters.presentationDefinition?.claims ?: listOf()

    storeContainerState?.let { storeContainer ->
        val credentialExtractor =
            CredentialExtractor(storeContainer.credentials.filter { it.scheme == requestedCredentialScheme })

        StatefulAuthenticationConsentView(
            spName = spName,
            spLocation = spLocation,
            spImage = spImage,
            requestedCredentialScheme = requestedCredentialScheme,
            requestedAttributes = requestedAttributes,
            authenticationRequestParameters = authenticationRequestParameters,
            credentialExtractor = credentialExtractor,
            fromQrCodeScanner = fromQrCodeScanner,
            navigateUp = navigateUp,
            navigateToRefreshCredentialsPage = {
                navigateToRefreshCredentialsPage(
                    RefreshRequirements(
                        authenticationRequestParametersStringified = jsonSerializer.encodeToString(
                            authenticationRequestParameters
                        )
                    )
                )
            },
            navigateToAuthenticationSuccessPage = navigateToAuthenticationSuccessPage,
            walletMain = walletMain,
        )
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun StatefulAuthenticationConsentView(
    spName: String,
    spLocation: String,
    spImage: ImageBitmap?,
    requestedCredentialScheme: ConstantIndex.CredentialScheme,
    requestedAttributes: List<String>,
    authenticationRequestParameters: AuthenticationRequestParameters,
    credentialExtractor: CredentialExtractor,
    fromQrCodeScanner: Boolean,
    navigateUp: () -> Unit,
    navigateToRefreshCredentialsPage: () -> Unit,
    navigateToAuthenticationSuccessPage: () -> Unit,
    walletMain: WalletMain,
) {
    val attributeCategorization = attributeCategorizationOrder.associateWith {
        it.attributes.get(requestedCredentialScheme) ?: listOf()
    }

    val categorizedClaims = attributeCategorization.toList().flatMap {
        it.second
    }

    val attributeCategorizationWithOthers = attributeCategorization + Pair(
        PersonalDataCategory.OtherData, requestedAttributes.filter {
            categorizedClaims.contains(it) == false
        }
    )

    val requestedAttributesLocalized =
        attributeCategorizationWithOthers.toList().map { attributeCategory ->
            Pair(
                attributeCategory.first,
                attributeCategory.second.mapNotNull { claim ->
                    if (requestedAttributes.contains(claim) == false) null else AttributeAvailability(
                        // also supports claims that are not supported yet (for example claims that may be added later on before the wallet is updated)
                        attributeName = AttributeTranslater(requestedCredentialScheme).translate(claim)
                            ?.let { stringResource(it) }
                            ?: claim,
                        isAvailable = credentialExtractor.containsAttribute(requestedCredentialScheme, claim),
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

val PresentationDefinition.claims: List<String>
    get() = this.inputDescriptors
        .mapNotNull { it.constraints }.flatMap { it.fields?.toList() ?: listOf() }
        .flatMap { it.path.toList() }
        .filter { it != "$.type" }
        .filter { it != "$.mdoc.doctype" }
        .filter { it != "$.mdoc.namespace" }
        .map { it.removePrefix("\$.mdoc.") }
        .map { it.removePrefix("\$.") }
        .map { it.removePrefix("\$['org.iso.18013.5.1']['").removeSuffix("']") }
        .map { it.removePrefix("\$['eu.europa.ec.eudiw.pid.1']['").removeSuffix("']") }