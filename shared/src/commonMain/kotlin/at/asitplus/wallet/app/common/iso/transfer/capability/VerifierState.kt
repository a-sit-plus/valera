package at.asitplus.wallet.app.common.iso.transfer.capability

sealed class VerifierState {
    object Init : VerifierState()
    object SelectDocument : VerifierState()
    object SelectCustomRequest : VerifierState()
    object SelectCombinedRequest : VerifierState()
    object QrEngagement : VerifierState()
    object WaitingForResponse : VerifierState()
    object CheckResponse : VerifierState()
    object Presentation : VerifierState()
    object Error : VerifierState()

    data class MissingPrecondition(val reason: PreconditionState) : VerifierState()
}
