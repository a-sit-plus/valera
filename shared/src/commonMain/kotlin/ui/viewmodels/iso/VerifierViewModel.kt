package ui.viewmodels.iso

import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.iso.transfer.DeviceEngagementMethods
import at.asitplus.wallet.app.common.iso.transfer.TransferManager
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.lib.iso.DeviceResponse
import at.asitplus.wallet.mdl.MobileDrivingLicenceDataElements
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import at.asitplus.wallet.app.common.iso.transfer.MdocConstants.MDOC_PREFIX
import data.document.RequestDocument
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ui.viewmodels.iso.SelectableAge.OVER_14
import ui.viewmodels.iso.SelectableAge.OVER_16
import ui.viewmodels.iso.SelectableAge.OVER_18
import ui.viewmodels.iso.SelectableAge.OVER_21

class VerifierViewModel(
    val navigateUp: () -> Unit,
    val onClickLogo: () -> Unit,
    val walletMain: WalletMain,
    val navigateToHomeScreen: () -> Unit,
    val onClickSettings: () -> Unit
) {
    private val transferManager: TransferManager by lazy {
        TransferManager(walletMain.scope) { message -> } // TODO: handle update messages
    }

    private val _verifierState = MutableStateFlow(VerifierState.INIT)
    val verifierState: StateFlow<VerifierState> = _verifierState

    fun setVerifierState(newVerifierState: VerifierState) {
        _verifierState.value = newVerifierState
    }

    private val _requestDocument = MutableStateFlow<RequestDocument?>(null)

    private val _deviceResponse = MutableStateFlow<DeviceResponse?>(null)
    val deviceResponse: StateFlow<DeviceResponse?> = _deviceResponse

    private val _errorMessage = MutableStateFlow<String>("")
    val errorMessage: StateFlow<String> = _errorMessage

    fun handleError(errorMessage: String) {
        _errorMessage.value = errorMessage
        setVerifierState(VerifierState.ERROR)
    }

    private val _selectedEngagementMethod = MutableStateFlow<DeviceEngagementMethods>(
        DeviceEngagementMethods.NFC
    )

    val selectedEngagementMethod: StateFlow<DeviceEngagementMethods> = _selectedEngagementMethod

    private fun setStateToEngagement(selectedEngagementMethod: DeviceEngagementMethods) {
        when (selectedEngagementMethod) {
            DeviceEngagementMethods.NFC -> doNfcEngagement()
            DeviceEngagementMethods.QR_CODE -> _verifierState.value = VerifierState.QR_ENGAGEMENT
        }
    }

    private fun doNfcEngagement() {
        _requestDocument.value?.let { document ->
            transferManager.startNfcEngagement(document) { deviceResponseBytes ->
                handleResponse(deviceResponseBytes)
            }
        }
    }

    private fun handleResponse(deviceResponseBytes: ByteArray) {
        _deviceResponse.value = DeviceResponse.deserialize(deviceResponseBytes).getOrThrow()
        _verifierState.value = VerifierState.PRESENTATION
    }

    fun onClickPredefinedMdl(selectedEngagementMethod: DeviceEngagementMethods) {
        _requestDocument.value = getMdlRequestDocument()
        setStateToEngagement(selectedEngagementMethod)
    }

    fun onClickPredefinedPid(selectedEngagementMethod: DeviceEngagementMethods) {
        _requestDocument.value = getPidRequestDocument()
        setStateToEngagement(selectedEngagementMethod)
    }

    fun onClickPredefinedAge(age: Int, selectedEngagementMethod: DeviceEngagementMethods) {
        _requestDocument.value = getAgeVerificationRequestDocument(age)
        setStateToEngagement(selectedEngagementMethod)
    }

    fun navigateToCustomSelectionView(selectedEngagementMethod: DeviceEngagementMethods) {
        _selectedEngagementMethod.value = selectedEngagementMethod
        _verifierState.value = VerifierState.SELECT_CUSTOM_REQUEST
    }

    fun navigateToVerifyDataView() {
        _verifierState.value = VerifierState.INIT
    }

    fun onReceiveCustomSelection(
        customSelectionDocument: RequestDocument,
        selectedEngagementMethod: DeviceEngagementMethods
    ) {
        _requestDocument.value = customSelectionDocument
        setStateToEngagement(selectedEngagementMethod)
    }

    val onFoundPayload: (String) -> Unit = { payload ->
        if (payload.startsWith(MDOC_PREFIX)) {
            _verifierState.value = VerifierState.WAITING_FOR_RESPONSE
            _requestDocument.value?.let { document ->
                transferManager.doQrFlow(
                    payload.substring(5),
                    document,
                    { message -> } // TODO: handle update messages
                ) { deviceResponseBytes ->
                    handleResponse(deviceResponseBytes)
                }
            }
        } else {
            val errorMessage = "Invalid QR-Code:\nQR-Code does not start with \"$MDOC_PREFIX\""
            Napier.e(errorMessage)
            handleError(errorMessage)
        }
    }
}

fun getMdlRequestDocument(): RequestDocument {
    return RequestDocument(
        docType = MobileDrivingLicenceScheme.isoDocType,
        itemsToRequest = mapOf(
            MobileDrivingLicenceScheme.isoNamespace to MobileDrivingLicenceDataElements.MANDATORY_ELEMENTS
                .associateWith { false }
        )
    )
}

fun getMdlPreselection(): Set<String> {
    return MobileDrivingLicenceDataElements.MANDATORY_ELEMENTS.toSet()
}

fun getPidRequestDocument(): RequestDocument {
    return RequestDocument(
        docType = EuPidScheme.isoDocType,
        itemsToRequest = mapOf(
            EuPidScheme.isoNamespace to EuPidScheme.requiredClaimNames
                .associateWith { false }
        )
    )
}

fun getPidPreselection(): Set<String> {
    return EuPidScheme.requiredClaimNames.toSet()
}

fun getAgeVerificationRequestDocument(age: Int): RequestDocument {
    val elementName = when(age) {
        OVER_14 -> MobileDrivingLicenceDataElements.AGE_OVER_14
        OVER_16 -> MobileDrivingLicenceDataElements.AGE_OVER_16
        OVER_18 -> MobileDrivingLicenceDataElements.AGE_OVER_18
        OVER_21 -> MobileDrivingLicenceDataElements.AGE_OVER_21
        else -> MobileDrivingLicenceDataElements.AGE_OVER_18
    }
    return RequestDocument(
        docType = MobileDrivingLicenceScheme.isoDocType,
        itemsToRequest = mapOf(
            MobileDrivingLicenceScheme.isoNamespace to mapOf(elementName to false)
        )
    )
}

fun itemsToRequestDocument(
    docType: String,
    namespace: String,
    entries: Set<String>
): RequestDocument {
    return RequestDocument(
        docType = docType,
        itemsToRequest = mapOf(
            namespace to entries.associateWith { false }
        )
    )
}

object SelectableAge {
    const val OVER_14 = 14
    const val OVER_16 = 16
    const val OVER_18 = 18
    const val OVER_21 = 21

    val values = listOf(OVER_14, OVER_16, OVER_18, OVER_21)
}

enum class VerifierState {
    INIT,
    SELECT_CUSTOM_REQUEST,
    QR_ENGAGEMENT,
    WAITING_FOR_RESPONSE,
    PRESENTATION,
    ERROR
}
