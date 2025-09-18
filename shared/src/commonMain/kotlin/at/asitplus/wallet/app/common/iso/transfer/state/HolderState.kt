package at.asitplus.wallet.app.common.iso.transfer.state

sealed class HolderState {
    object Init : HolderState()
    object Settings : HolderState()
    object CheckSettings : HolderState()
    object CreateEngagement : HolderState()
    object ShowQrCode : HolderState()
    object Finished : HolderState()

    data class MissingPrecondition(val reason: PreconditionState) : HolderState()
}
