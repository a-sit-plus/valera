package ui.views.intents

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import at.asitplus.catchingUnwrapped
import at.asitplus.dcapi.ios.IosDcApiMdocPreRequestSummary
import at.asitplus.wallet.app.common.IntentState
import at.asitplus.wallet.app.common.LoadingMessageKey
import kotlinx.serialization.json.Json
import ui.presentation.AuthenticationReceivedStartPageContent
import ui.views.LoadingView
import ui.views.loadingMessageString

@Composable
fun IosDcApiPreRequestView(
    intentState: IntentState,
    onError: (Throwable) -> Unit,
) {
    val preRequestData by intentState.iosDcApiPreRequestData.collectAsState()

    // Null is expected during initial loading — show a spinner without reporting an error.
    // Only report an error once we have data but the summary is missing/unparseable.
    val currentData = preRequestData ?: return LoadingView(loadingMessageString(LoadingMessageKey.IncomingRequest))

    val parsedSummaryResult = remember(currentData.parsedRequestSummary) {
        catchingUnwrapped {
            Json.decodeFromString<IosDcApiMdocPreRequestSummary>(
                currentData.parsedRequestSummary
                    ?: throw IllegalStateException("No parsed request summary available")
            )
        }
    }

    parsedSummaryResult.exceptionOrNull()?.let { error ->
        LaunchedEffect(error) {
            onError(error)
        }
        return LoadingView(loadingMessageString(LoadingMessageKey.IncomingRequest))
    }

    val parsedSummary = parsedSummaryResult.getOrNull() ?: run {
        LaunchedEffect(Unit) { onError(IllegalStateException("Missing parsed request summary")) }
        return LoadingView(loadingMessageString(LoadingMessageKey.IncomingRequest))
    }
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
