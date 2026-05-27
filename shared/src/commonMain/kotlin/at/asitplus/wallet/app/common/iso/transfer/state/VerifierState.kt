package at.asitplus.wallet.app.common.iso.transfer.state

sealed class VerifierState {
    object Settings : VerifierState()
    object CheckSettings : VerifierState()
    object SelectDocument : VerifierState()
    object SelectCustomRequest : VerifierState()
    object SelectCombinedRequest : VerifierState()
    object QrEngagement : VerifierState()
    // Waiting for the user to tap the verifier device to the holder's NFC field
    object NfcEngagement : VerifierState()
    // NFC handover done; data transfer in progress
    // isNfc=true → NFC data transfer (keep devices in field)
    // isNfc=false → BLE data transfer (devices can move apart)
    data class NfcTransferring(val isNfc: Boolean) : VerifierState()
    object WaitingForResponse : VerifierState()
    object CheckResponse : VerifierState()
    object Presentation : VerifierState()
    object Error : VerifierState()

    data class MissingPrecondition(val reason: PreconditionState) : VerifierState()
}
