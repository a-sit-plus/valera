package ui.viewmodels.authentication

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.catchingUnwrapped
import at.asitplus.iso.DeviceRequest
import at.asitplus.iso.SessionTranscript
import at.asitplus.signum.indispensable.pki.CertificateChain
import at.asitplus.signum.indispensable.pki.X509Certificate
import at.asitplus.signum.supreme.UserInitiatedCancellationReason
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.warning_authentication_cancelled
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.extractCertificateChain
import at.asitplus.wallet.app.common.relyingParty.RelyingPartyValidationUi
import at.asitplus.wallet.app.common.validation.integrityRejection
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.CredentialPresentation
import at.asitplus.wallet.lib.data.CredentialPresentationRequest
import at.asitplus.wallet.lib.ktor.openid.OpenId4VpKtorHolder
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

class PresentationViewModel(
    val presentationStateModel: PresentationStateModel,
    navigateUp: () -> Unit,
    onAuthenticationSuccess: (redirectUrl: String?) -> Unit,
    navigateToHomeScreen: () -> Unit,
    walletMain: WalletMain,
    spImage: ImageBitmap? = null,
    onClickLogo: () -> Unit
) : AuthenticationViewModel(
    spName = null,
    spLocation = "Local Presentation",
    spImage,
    navigateUp,
    onAuthenticationSuccess,
    navigateToHomeScreen,
    walletMain,
    onClickLogo
) {
    private var deviceRequest: DeviceRequest? = null
    private var finishFunction: ((ByteArray) -> Unit)? = null
    private var sessionTranscript: SessionTranscript? = null
    var verifierCertificateChain: CertificateChain? = null

    /**
     * The validation of the reader, `null` for a request without reader authentication, or without a session
     * transcript to verify it over.
     */
    var relyingPartyValidation: RelyingPartyValidationUi? = null

    /**
     * Validates the relying party of [parsedRequest], i.e. its reader authentication over [sessionTranscript], and
     * fails if its integrity is not intact, e.g. its signature. Other problems, e.g. an untrusted reader, are shown to
     * the user as warnings.
     */
    suspend fun initWithDeviceRequest(
        parsedRequest: DeviceRequest,
        finishFunction: (ByteArray) -> Unit,
        sessionTranscript: SessionTranscript?
    ) {
        // VC-K rejects the relying party of a request without reader authentication, or without WRPRC, by its
        // `requestData`, which blocks, so such requests are not validated, as VC-K reports no relying party for
        // OpenID4VP and DC API requests without WRPRC either
        val authenticated = !parsedRequest.readerAuthAll.isNullOrEmpty() ||
                parsedRequest.docRequests.any { it.readerAuth != null }
        val registered = parsedRequest.docRequests.any { it.itemsRequest.value.requestInfo?.euWrprc != null }
        val validation = sessionTranscript?.takeIf { authenticated && registered }?.let {
            walletMain.presentationService.validateRelyingParty(parsedRequest, it)
        }
        val relyingParty = validation?.let { RelyingPartyValidationUi.of(it) }
        if (relyingParty?.outcome?.integrityFailed == true) {
            throw validation.report.integrityRejection() ?: IllegalArgumentException("The reader has been rejected")
        }
        deviceRequest = parsedRequest
        this.finishFunction = finishFunction
        this.sessionTranscript = sessionTranscript
        this.relyingPartyValidation = relyingParty
        this.verifierCertificateChain = parsedRequest.extractCertificateChain()
    }

    override val transactionData = null

    override val presentationRequest: CredentialPresentationRequest.IsoDeviceRetrieval
        get() = CredentialPresentationRequest.IsoDeviceRetrieval(
            requireNotNull(deviceRequest) { "No ISO device request initialized" }
        )

    override suspend fun findMatchingCredentials(): Result<at.asitplus.wallet.lib.agent.CredentialMatchingResult<SubjectCredentialStore.StoreEntry>> =
        catchingUnwrapped {
            walletMain.holderAgent.matchPresentationRequestAgainstCredentialStore(presentationRequest).getOrThrow()
        }

    override suspend fun finalizationMethod(credentialPresentation: CredentialPresentation) =
        finishFunction?.let {
            walletMain.presentationService.finalizeLocalPresentation(
                credentialPresentation = credentialPresentation as? CredentialPresentation.IsoDeviceRetrievalPresentation
                    ?: throw IllegalArgumentException("ISO proximity presentation requires a DeviceRequest submission"),
                it,
                spName,
                sessionTranscript!!
            )
            OpenId4VpKtorHolder.AuthenticationSuccess(null)
        } ?: throw IllegalStateException("No finish method found")

    override fun onCancel() {
        presentationStateModel.dismiss(PresentationStateModel.DismissType.CLICK)
    }

    override fun onError(error: Throwable) {
        if (error.isRecoverableAuthenticationCancel()) {
            walletMain.scope.launch {
                walletMain.snackbarService.showSnackbar(getString(Res.string.warning_authentication_cancelled))
            }
            return
        }
        presentationStateModel.dismiss(PresentationStateModel.DismissType.CLICK)
        super.onError(error)
    }

    private fun Throwable.isRecoverableAuthenticationCancel(): Boolean =
        generateSequence(this as Throwable?) { it.cause }.any { current ->
            current is UserInitiatedCancellationReason ||
                    (current::class.simpleName == "UnlockFailed" &&
                            current.message?.contains("Cancel", ignoreCase = true) == true) ||
                    current.message?.contains("No signature from native code", ignoreCase = true) == true
        }

    override fun handleAuthenticationSuccess(result: OpenId4VpKtorHolder.AuthenticationSuccess) {
        // Local presentment must keep the sheet alive until the presenter coroutine has sent the
        // device response and the transport flow completes.
    }

}
