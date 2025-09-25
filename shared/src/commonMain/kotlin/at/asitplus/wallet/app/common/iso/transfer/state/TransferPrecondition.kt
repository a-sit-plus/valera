package at.asitplus.wallet.app.common.iso.transfer.state

sealed interface TransferPrecondition {
    data object Ok : TransferPrecondition
    data object NoTransferMethodSelected : TransferPrecondition
    data object BleSelectedButNotEnabled : TransferPrecondition
    data object NfcSelectedButNotEnabled : TransferPrecondition
    data object MissingPermission : TransferPrecondition
    data object NfcEngagementNotAvailable : TransferPrecondition
}

enum class PreconditionState {
    OK,
    NO_TRANSFER_METHOD_SELECTED,
    BLE_SELECTED_BUT_NOT_ENABLED,
    NFC_SELECTED_BUT_NOT_ENABLED,
    MISSING_PERMISSION,
    NFC_ENGAGEMENT_NOT_AVAILABLE
}

fun TransferPrecondition.toEnum(): PreconditionState = when (this) {
    TransferPrecondition.Ok -> PreconditionState.OK
    TransferPrecondition.NoTransferMethodSelected -> PreconditionState.NO_TRANSFER_METHOD_SELECTED
    TransferPrecondition.BleSelectedButNotEnabled -> PreconditionState.BLE_SELECTED_BUT_NOT_ENABLED
    TransferPrecondition.NfcSelectedButNotEnabled -> PreconditionState.NFC_SELECTED_BUT_NOT_ENABLED
    TransferPrecondition.MissingPermission -> PreconditionState.MISSING_PERMISSION
    TransferPrecondition.NfcEngagementNotAvailable -> PreconditionState.NFC_ENGAGEMENT_NOT_AVAILABLE
}

fun evaluateTransferPrecondition(
    transferSettingsState: TransferSettingsState,
    bleEnabled: Boolean,
    blePermissionGranted: Boolean,
    nfcEnabled: Boolean,
    nfcEngagementSelected: Boolean = false
): TransferPrecondition {
    return when {
        !(transferSettingsState.isAnySettingOn) -> TransferPrecondition.NoTransferMethodSelected

        transferSettingsState.ble.required && !blePermissionGranted ->
            TransferPrecondition.MissingPermission

        transferSettingsState.ble.settingOn && !bleEnabled ->
            TransferPrecondition.BleSelectedButNotEnabled

        transferSettingsState.nfc.settingOn && !nfcEnabled ->
            TransferPrecondition.NfcSelectedButNotEnabled

        nfcEngagementSelected && !nfcEnabled -> TransferPrecondition.NfcEngagementNotAvailable

        else -> TransferPrecondition.Ok
    }
}
