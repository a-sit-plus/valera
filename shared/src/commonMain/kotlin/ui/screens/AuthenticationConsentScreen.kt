package ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.idaustria.IdAustriaScheme
import composewalletapp.shared.generated.resources.ERROR_AUTHENTICATION_AT_SP_FAILED
import composewalletapp.shared.generated.resources.Res
import data.CredentialExtractor
import data.idAustriaAttributeTranslation
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import ui.composables.AttributeAvailability
import ui.composables.PersonalDataCategory
import ui.views.AuthenticationConsentView

@OptIn(ExperimentalResourceApi::class)
@Composable
fun AuthenticationConsentScreen(
    spName: String,
    spLocation: String,
    spImage: ImageBitmap?,
    claims: List<String>,
    url: String,
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

        val claimAvailabilities = claims.associateWith { claim ->
            AttributeAvailability(
                attributeName = claim,
                isAvailable = credentialExtractor.containsIdAustriaAttribute(claim)
            )
        }

        val requestedAttributes = listOf(
            Pair(
                PersonalDataCategory.IdentityData, listOfNotNull(
                    claimAvailabilities[IdAustriaScheme.Attributes.FIRSTNAME],
                    claimAvailabilities[IdAustriaScheme.Attributes.LASTNAME],
                    claimAvailabilities[IdAustriaScheme.Attributes.DATE_OF_BIRTH],
                    claimAvailabilities[IdAustriaScheme.Attributes.PORTRAIT],
                )
            ),
            Pair(
                PersonalDataCategory.AgeData, listOfNotNull(
                    claimAvailabilities[IdAustriaScheme.Attributes.AGE_OVER_14],
                    claimAvailabilities[IdAustriaScheme.Attributes.AGE_OVER_16],
                    claimAvailabilities[IdAustriaScheme.Attributes.AGE_OVER_18],
                    claimAvailabilities[IdAustriaScheme.Attributes.AGE_OVER_21],
                )
            ),
            Pair(
                PersonalDataCategory.ResidenceData, listOfNotNull(
                    claimAvailabilities[IdAustriaScheme.Attributes.MAIN_ADDRESS],
                )
            ),
        ).filter { it.second.isNotEmpty() }.map {
            Pair(
                it.first,
                it.second.map {
                    AttributeAvailability(
                        attributeName = stringResource(it.attributeName.idAustriaAttributeTranslation),
                        isAvailable = it.isAvailable,
                    )
                }
            )
        }

        var showBiometry by rememberSaveable { mutableStateOf(false) }

        AuthenticationConsentView(
            spName = spName,
            spLocation = spLocation,
            spImage = spImage,
            requestedAttributes = requestedAttributes,
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
                        walletMain.presentationService.startSiop(url, fromQrCodeScanner)
                        navigateUp()
                        navigateToAuthenticationSuccessPage()
                    } catch (e: Throwable) {
                        walletMain.errorService.emit(e)
                        walletMain.snackbarService.showSnackbar(getString(Res.string.ERROR_AUTHENTICATION_AT_SP_FAILED))
                    }
                }
            },
            onBiometryDismissed = { biometryPromptDismissResult ->
                walletMain.snackbarService.showSnackbar(biometryPromptDismissResult.errorString)
                showBiometry = false
            },
        )
    }
}