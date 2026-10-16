package ui.views.intents

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import at.asitplus.catchingUnwrapped
import at.asitplus.dcapi.ios.IosDcApiMdocPreRequestSummary
import at.asitplus.signum.indispensable.pki.CertificateChain
import at.asitplus.signum.indispensable.pki.X509Certificate
import at.asitplus.wallet.app.common.Configuration
import at.asitplus.wallet.app.common.IntentState
import at.asitplus.wallet.app.common.LoadingMessageKey
import at.asitplus.wallet.app.common.TrustListService
import at.asitplus.wallet.lib.data.CredentialPresentationRequest
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.Json
import ui.composables.TrustState
import ui.presentation.AuthenticationReceivedStartPageContent
import ui.views.LoadingView
import ui.views.loadingMessageString
import kotlin.io.encoding.Base64

@Composable
fun IosDcApiPreRequestView(
    intentState: IntentState,
    onError: (Throwable) -> Unit,
    trustListService: TrustListService
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
    val origin = currentData.origin

    // Fetch missing or expired Trust Lists, which this process may not have refreshed yet; the trust state below
    // observes the persisted lists, and updates when they arrive
    LaunchedEffect(trustListService) {
        trustListService.refreshStaleTrustLists(Configuration.TRUST_LIST_REFRESH_TIMEOUT)
    }
    val preliminaryRelyingPartyTrust = remember(trustListService, currentData.readerCertificateChains) {
        currentData.readerCertificateChains.firstNotNullOfOrNull { it.decodeCertificateChain() }
            ?.let { trustListService.observeTrustStateForCertChain(flowOf(it)) }
            ?: flowOf(TrustState.UNKNOWN)
    }

    AuthenticationReceivedStartPageContent(
        authenticateAtRelyingParty = !origin.isNullOrBlank() && origin != "Local Presentation",
        serviceProviderLogo = null,
        serviceProviderLocalizedName = null,
        serviceProviderLocalizedLocation = origin,
        onAbort = currentData.onCancel,
        onContinue = currentData.onContinue,
        presentationRequest = CredentialPresentationRequest.IsoDeviceRetrieval(parsedSummary.toDeviceRequest()),
        errorAction = onError,
        trustListService = trustListService,
        // Apple's parsed request does not expose zero-knowledge information,
        // the full request is only available after continuing
        showZkMode = false,
        preliminaryRelyingPartyTrust = preliminaryRelyingPartyTrust,
    )
}

/** The certificate chain of Base64 encoded DER certificates, `null` if any of them can not be decoded. */
private fun List<String>.decodeCertificateChain(): CertificateChain? = catchingUnwrapped {
    map { X509Certificate.decodeFromDer(Base64.decode(it)) }
}.onFailure {
    Napier.w("Could not decode reader certificate chain of the parsed DC API request", it)
}.getOrNull()?.takeIf { it.isNotEmpty() }
