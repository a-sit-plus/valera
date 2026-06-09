package at.asitplus.wallet.app.common.iso.transfer.state

enum class TransferTransport { NFC, BLUETOOTH }

sealed class VerifierState {
    object Settings : VerifierState()
    object CheckSettings : VerifierState()
    object SelectDocument : VerifierState()
    object SelectCustomRequest : VerifierState()
    object SelectCombinedRequest : VerifierState()
    object QrEngagement : VerifierState()
    // Waiting for the user to tap the verifier device to the holder's NFC field
    object NfcEngagement : VerifierState()
    // QR engagement is done; waiting for the user to tap devices for NFC data transfer
    object QrNfcDataTransferTap : VerifierState()
    // Handover done; data transfer in progress over the given transport
    data class NfcTransferring(val transport: TransferTransport) : VerifierState()
    object WaitingForResponse : VerifierState()
    object CheckResponse : VerifierState()
    object Presentation : VerifierState()
    object Error : VerifierState()

    data class MissingPrecondition(val reason: PreconditionState) : VerifierState()
}
