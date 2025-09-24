package at.asitplus.wallet.app.common.iso.transfer.state

sealed interface TransferPrecondition {
    data object Ok : TransferPrecondition
    data object NoTransferMethodSelected : TransferPrecondition
    data object NoTransferMethodAvailable : TransferPrecondition
    data object MissingPermission : TransferPrecondition
    data object NfcEngagementNotAvailable : TransferPrecondition
}

enum class PreconditionState {
    OK,
    NO_TRANSFER_METHOD_SELECTED,
    NO_TRANSFER_METHOD_AVAILABLE_FOR_SELECTION,
    MISSING_PERMISSION,
    NFC_ENGAGEMENT_NOT_AVAILABLE
}

fun TransferPrecondition.toEnum(): PreconditionState = when (this) {
    TransferPrecondition.Ok -> PreconditionState.OK
    TransferPrecondition.NoTransferMethodSelected ->
        PreconditionState.NO_TRANSFER_METHOD_SELECTED
    TransferPrecondition.NoTransferMethodAvailable ->
        PreconditionState.NO_TRANSFER_METHOD_AVAILABLE_FOR_SELECTION
    TransferPrecondition.MissingPermission ->
        PreconditionState.MISSING_PERMISSION
    TransferPrecondition.NfcEngagementNotAvailable ->
        PreconditionState.NFC_ENGAGEMENT_NOT_AVAILABLE
}

fun evaluateTransferPrecondition(
    transferSettingsState: TransferSettingsState,
    bleEnabled: Boolean,
    blePermissionGranted: Boolean,
    nfcEnabled: Boolean,
    nfcEngagementSelected: Boolean
): TransferPrecondition {
    return when {
        !(transferSettingsState.isAnySettingOn) -> TransferPrecondition.NoTransferMethodSelected

        transferSettingsState.ble.required && !blePermissionGranted ->
            TransferPrecondition.MissingPermission

        (transferSettingsState.ble.settingOn && !bleEnabled) ||
        (transferSettingsState.nfc.settingOn && !nfcEnabled) ->
            TransferPrecondition.NoTransferMethodAvailable

        nfcEngagementSelected && !transferSettingsState.nfc.settingOn ->
            TransferPrecondition.NfcEngagementNotAvailable

        else -> TransferPrecondition.Ok
    }
}
