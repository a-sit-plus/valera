package ui.views.intents

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import at.asitplus.wallet.app.common.IntentState
import at.asitplus.wallet.app.dcapi.IosParsedMdocRequestSummary
import kotlinx.serialization.json.Json
import ui.presentation.AuthenticationReceivedStartPageContent
import ui.views.LoadingView

@Composable
fun IosDcApiPreRequestView(
    intentState: IntentState,
    onError: (Throwable) -> Unit,
) {
    val preRequestData by intentState.iosDcApiPreRequestData.collectAsState()

    val parsedSummaryResult = remember(preRequestData?.parsedRequestSummary) {
        runCatching {
            Json.decodeFromString<IosParsedMdocRequestSummary>(
                preRequestData?.parsedRequestSummary
                    ?: throw IllegalStateException("No parsed request summary available")
            )
        }
    }

    parsedSummaryResult.exceptionOrNull()?.let { error ->
        LaunchedEffect(error) {
            onError(error)
        }
        return LoadingView()
    }

    val currentData = preRequestData ?: return LoadingView()
    val parsedSummary = parsedSummaryResult.getOrNull() ?: return LoadingView()
    val descriptors = parsedSummary.toDifInputDescriptors()
    val origin = currentData.origin

    AuthenticationReceivedStartPageContent(
        authenticateAtRelyingParty = !origin.isNullOrBlank() && origin != "Local Presentation",
        serviceProviderLogo = null,
        serviceProviderLocalizedName = null,
        serviceProviderLocalizedLocation = origin,
        onAbort = currentData.onCancel,
        onContinue = currentData.onContinue,
        presentationRequest = null,
        inputDescriptors = descriptors,
        onError = onError,
    )
}
