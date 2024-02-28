package view

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.wallet.app.common.WalletMain
import data.IdAustriaAttribute
import data.containsIdAustriaAttribute
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import ui.composables.AttributeAvailability
import ui.composables.PersonalDataCategory
import ui.views.AuthenticationConsentView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticationConsentScreen(
    spName: String,
    spLocation: String,
    spImage: ImageBitmap?,
    claims: List<String>,
    url: String,
    navigateUp: () -> Unit,
    navigateToRefreshCredentialsPage: () -> Unit,
    navigateToAuthenticationSuccessPage: () -> Unit,
    walletMain: WalletMain,
) {
    val storeContainerState by walletMain.subjectCredentialStore.observeStoreContainer()
        .collectAsState(null)

    storeContainerState?.let { storeContainer ->
        val credentials = storeContainer.credentials

        val claimAvailabilities = claims.associateWith { claim ->
            AttributeAvailability(
                attributeName = claim,
                isAvailable = credentials.firstOrNull {
                    it.containsIdAustriaAttribute(claim)
                } != null
            )
        }

        val requestedAttributes = listOf(
            Pair(
                PersonalDataCategory.IdentityData, listOfNotNull(
                    claimAvailabilities[IdAustriaAttribute.FirstName.attributeName],
                    claimAvailabilities[IdAustriaAttribute.LastName.attributeName],
                    claimAvailabilities[IdAustriaAttribute.DateOfBirth.attributeName],
                )
            ),
            Pair(
                PersonalDataCategory.AgeData, listOfNotNull(
                    claimAvailabilities[IdAustriaAttribute.AgeAtLeast14.attributeName],
                    claimAvailabilities[IdAustriaAttribute.AgeAtLeast16.attributeName],
                    claimAvailabilities[IdAustriaAttribute.AgeAtLeast18.attributeName],
                    claimAvailabilities[IdAustriaAttribute.AgeAtLeast21.attributeName],
                )
            ),
            Pair(
                PersonalDataCategory.ResidenceData, listOfNotNull(
                    claimAvailabilities[IdAustriaAttribute.MainAddress.attributeName],
//                    claimAvailabilities[IdAustriaAttribute.PostalCode.attributeName],
//                    claimAvailabilities[IdAustriaAttribute.TownName.attributeName],
                )
            ),
            Pair(
                PersonalDataCategory.DrivingPermissions, listOfNotNull(
                    claimAvailabilities[IdAustriaAttribute.DrivingPermissions.attributeName],
                )
            ),
            Pair(
                PersonalDataCategory.AdmissionData, listOfNotNull(
                    claimAvailabilities[IdAustriaAttribute.CarModel.attributeName],
                    claimAvailabilities[IdAustriaAttribute.LicensePlateNumber.attributeName],
                )
            ),
        ).filter { it.second.isNotEmpty() }.map {
            Pair(
                it.first,
                it.second.map {
                    AttributeAvailability(
                        attributeName = IdAustriaAttribute.attributeTranslation(
                            it.attributeName
                        ),
                        isAvailable = it.isAvailable,
                    )
                }
            )
        }

        var showBiometry by remember { mutableStateOf(false) }

        AuthenticationConsentView(
//            spUrl = url,
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
                        walletMain.presentationService.startSiop(url)
                        navigateUp()
                        navigateToAuthenticationSuccessPage()
                    } catch (e: Throwable) {
                        walletMain.errorService.emit(e)
                        walletMain.snackbarService.showSnackbar("Authentication failed")
                    }
                }
            },
            onBiometryDismissed = {
                showBiometry = false
            },
        )
    }
}