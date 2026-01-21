package ui.presentation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.openid.TransactionDataBase64Url
import ui.views.authentication.TransactionDataView

@ExperimentalMaterial3Api
@Composable
fun AuthenticationFinalizationPage(
    onNavigateUp: () -> Unit,
    onContinue: () -> Unit,
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    onAbort: () -> Unit = onNavigateUp,
    serviceProviderLogo: ImageBitmap?,
    serviceProviderLocalizedName: String?,
    serviceProviderLocalizedLocation: String,
    transactionData: TransactionDataBase64Url?,
) {
    AuthenticationFinalizationPageScaffold(
        onNavigateUp = onNavigateUp,
        onContinue = onContinue,
        onClickSettings = onClickSettings,
        onClickLogo = onClickLogo,
        onAbort = onAbort,
    ) {
        AuthenticationFinalizationPageContent(
            authenticateAtRelyingParty = serviceProviderLocalizedLocation != "Local Presentation",
            serviceProviderLogo = serviceProviderLogo,
            serviceProviderLocalizedName = serviceProviderLocalizedName,
            serviceProviderLocalizedLocation = serviceProviderLocalizedLocation,
            additionalDataView = transactionData?.let {
                {
                    TransactionDataView(it)
                }
            }
        )
    }
}