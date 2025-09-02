package at.asitplus.wallet.app.common.iso.transfer.capability

sealed class ShowQrCodeState {
    object Init : ShowQrCodeState()
    object CreateEngagement : ShowQrCodeState()
    object ShowQrCode : ShowQrCodeState()
    object Finished : ShowQrCodeState()

    data class MissingPrecondition(val reason: PreconditionState) : ShowQrCodeState()
}
