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
import at.asitplus.wallet.app.common.relyingParty.RelyingPartyStatusResolution
import at.asitplus.wallet.app.common.relyingParty.RelyingPartyValidationUi
import at.asitplus.wallet.app.common.validation.integrityRejection
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.CredentialPresentation
import at.asitplus.wallet.lib.data.CredentialPresentationRequest
import at.asitplus.wallet.lib.ktor.openid.OpenId4VpKtorHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import ui.presentation.RelyingPartyStatusGate

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

    private val mutableRelyingParty = MutableStateFlow<RelyingPartyStatusResolution?>(null)

    /**
     * The validation of the reader of the current request, whose status is resolved in the background, `null` for a
     * request without reader authentication, or without a session transcript to verify it over.
     */
    val relyingParty: StateFlow<RelyingPartyStatusResolution?> = mutableRelyingParty.asStateFlow()

    private val gate = RelyingPartyStatusGate(relyingParty, ::submitSelection)

    /** Holds back a presentation confirmed while the status of the reader is pending */
    val statusGate: RelyingPartyStatusGate<*>
        get() = gate

    /** The selection confirmed by the user, `null` for the selection by default */
    private class Selection(
        val submissions: CredentialPresentationSubmissions<SubjectCredentialStore.StoreEntry>?,
    )

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
        // VC-K rejects the relying party of a request without reader authentication by its `requestData`, which
        // blocks, so such requests are not validated; a request without WRPRC only fails its `registration`
        val authenticated = !parsedRequest.readerAuthAll.isNullOrEmpty() ||
                parsedRequest.docRequests.any { it.readerAuth != null }
        val pending = sessionTranscript?.takeIf { authenticated }?.let {
            walletMain.presentationService.validateRelyingParty(parsedRequest, it)
        }
        val validation = pending?.withoutStatus
        val relyingParty = validation?.let { RelyingPartyValidationUi.of(it) }
        // A pending status never fails the integrity, so this is decided before it is resolved
        if (relyingParty?.outcome?.integrityFailed == true) {
            throw validation.report.integrityRejection() ?: IllegalArgumentException("The reader has been rejected")
        }
        deviceRequest = parsedRequest
        this.finishFunction = finishFunction
        this.sessionTranscript = sessionTranscript
        val resolution = pending?.let {
            RelyingPartyStatusResolution.start(relyingParty, it.isPending, walletMain.scope) {
                RelyingPartyValidationUi.of(it.resolveStatus())
            }
        }
        // A reader may send several requests in one session: the status of the previous one is not needed any more
        mutableRelyingParty.getAndUpdate { resolution }?.cancel()
        this.verifierCertificateChain = parsedRequest.extractCertificateChain()
    }

    override fun confirmSelection(
        credentialPresentationSubmissions: CredentialPresentationSubmissions<SubjectCredentialStore.StoreEntry>?,
    ) = gate.confirm(Selection(credentialPresentationSubmissions))

    private fun submitSelection(selection: Selection) = super.confirmSelection(selection.submissions)

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
        mutableRelyingParty.value?.cancel()
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
