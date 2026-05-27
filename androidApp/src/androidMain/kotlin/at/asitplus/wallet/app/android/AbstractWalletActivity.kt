package at.asitplus.wallet.app.android

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CreateDigitalCredentialResponse
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.credentials.DigitalCredential
import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.CreateCredentialCustomException
import androidx.credentials.exceptions.GetCredentialCustomException
import androidx.credentials.provider.PendingIntentHandler
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.wallet.app.common.presentation.NfcDispatchSuppressionMode
import at.asitplus.wallet.app.common.presentation.NfcTransferState
import at.asitplus.wallet.app.common.dcapi.data.ErrorResponse
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.combine
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.multipaz.context.initializeApplication
import java.security.Security

abstract class AbstractWalletActivity : AppCompatActivity() {

    abstract fun populateLink(intent: Intent)

    private val holderForegroundDispatchReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Napier.d("Suppressing holder NFC foreground dispatch action=${intent?.action}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        populateLink(intent)
        initMultipaz()
    }

    private fun initMultipaz() {
        // required for identity.Crypto classes
        Security.removeProvider("BC")
        Security.addProvider(BouncyCastleProvider())

        initializeApplication(this.applicationContext)
    }

    @OptIn(ExperimentalDigitalCredentialApi::class)
    fun sendCredentialResponseToDCAPIInvoker(resultStr: String, success: Boolean) {
        // TODO use credentials API error types
        val resultData = Intent()

        if (success) {
            // androidx credentials library
            val credential = try {
                DigitalCredential(resultStr)
            } catch (e: IllegalArgumentException) {
                Napier.e("Failed to create response", e)
                val errorResponse = joseCompliantSerializer.encodeToString(ErrorResponse("internal error"))
                sendErrorResponse(errorResponse, resultData)
                null
            }

            credential?.let {
                PendingIntentHandler.setGetCredentialResponse(
                    resultData,
                    GetCredentialResponse(it)
                )
            }
        } else {
            Napier.e("Creating error response for DC API. Error: $resultStr")
            sendErrorResponse(resultStr, resultData)
        }

        Napier.d("Successfully returned response to DC API invoker. Response: $resultStr")
        setResult(RESULT_OK, resultData)
        finish()
    }

    @OptIn(ExperimentalDigitalCredentialApi::class)
    fun sendCredentialCreationResponseToDCAPIInvoker(resultStr: String, success: Boolean) {
        // TODO use credentials API error types
        val resultData = Intent()

        if (success) {
            PendingIntentHandler.setCreateCredentialResponse(
                resultData, CreateDigitalCredentialResponse(resultStr)
            )
        } else {
            Napier.e("Creating error response for DC API. Error: $resultStr")
            PendingIntentHandler.setCreateCredentialException(
                resultData, CreateCredentialCustomException(resultStr, resultStr)
            )
        }

        Napier.d("Successfully returned response to DC API invoker. Response: $resultStr")
        setResult(RESULT_OK, resultData)
        finish()
    }

    private fun sendErrorResponse(resultStr: String, resultData: Intent) {
        /* TODO check with SP that supports exceptions whether this works
              * otherwise try with the Google GMS library
            */
        Napier.v("Returning error response: $resultStr")
        PendingIntentHandler.setGetCredentialException(
            resultData,
            GetCredentialCustomException(
                resultStr, resultStr
            )
        )
    }

    private var preferredServiceJob: Job? = null
    private var verifierNfcJob: Job? = null
    private var verifierTagDispatchJob: Job? = null
    private var holderForegroundDispatchEnabled = false
    private var holderForegroundDispatchReceiverRegistered = false

    override fun onResume() {
        super.onResume()
        val nfcAdapter = NfcAdapter.getDefaultAdapter(this) ?: return
        val cardEmulation = CardEmulation.getInstance(nfcAdapter)
        if (!cardEmulation.categoryAllowsForegroundPreference(CardEmulation.CATEGORY_OTHER)) {
            Napier.w("CardEmulation.categoryAllowsForegroundPreference(CATEGORY_OTHER) returned false")
        }
        preferredServiceJob = lifecycleScope.launch {
            combine(
                NfcTransferState.nfcDataTransferActive,
                NfcTransferState.verifierNfcTransferActive,
                NfcTransferState.verifierNfcTagDispatchSuppressed,
            ) { dataTransfer, verifierTransfer, suppressionMode ->
                Triple(dataTransfer, verifierTransfer, suppressionMode)
            }.collect { (dataTransfer, verifierTransfer, suppressionMode) ->
                if (verifierTransfer) {
                    Napier.i("Verifier NFC reader active; unsetting preferred NFC HCE service")
                    if (!cardEmulation.unsetPreferredService(this@AbstractWalletActivity)) {
                        Napier.w("CardEmulation.unsetPreferredService() returned false")
                    }
                    return@collect
                }
                if (suppressionMode != NfcDispatchSuppressionMode.NONE) {
                    Napier.i("NFC dispatch suppressed; unsetting preferred NFC HCE service")
                    if (!cardEmulation.unsetPreferredService(this@AbstractWalletActivity)) {
                        Napier.w("CardEmulation.unsetPreferredService() returned false")
                    }
                    return@collect
                }
                val serviceClass = if (dataTransfer) NfcDataRetrievalService::class.java
                else NdefDeviceEngagementService::class.java
                Napier.i(
                    "Setting preferred NFC HCE service to ${serviceClass.simpleName}; dataTransfer=$dataTransfer"
                )
                if (!cardEmulation.setPreferredService(
                        this@AbstractWalletActivity,
                        ComponentName(this@AbstractWalletActivity, serviceClass)
                    )
                ) {
                    Napier.w("CardEmulation.setPreferredService() returned false for ${serviceClass.simpleName}")
                } else {
                    Napier.i("Preferred NFC HCE service set to ${serviceClass.simpleName}")
                }
            }
        }
        verifierNfcJob = lifecycleScope.launch {
            var seenActive = false
            NfcTransferState.verifierNfcTransferActive.collect { isActive ->
                if (isActive) {
                    NfcTransferState.verifierNfcTagDispatchSuppressed.value = NfcDispatchSuppressionMode.NONE
                    Napier.i("Verifier NFC data transfer active; keeping reader mode enabled")
                    seenActive = true
                } else if (seenActive) {
                    seenActive = false
                    Napier.i("Verifier NFC data transfer ended; suppressing system tag redispatch")
                    NfcTransferState.verifierNfcTagDispatchSuppressed.value = NfcDispatchSuppressionMode.REDISPATCH
                }
            }
        }
        verifierTagDispatchJob = lifecycleScope.launch {
            combine(
                NfcTransferState.nfcDataTransferActive,
                NfcTransferState.verifierNfcTransferActive,
                NfcTransferState.verifierNfcTagDispatchSuppressed,
            ) { holderTransfer, verifierTransfer, suppressionMode ->
                Triple(holderTransfer, verifierTransfer, suppressionMode)
            }.collect { (holderTransfer, verifierTransfer, suppressionMode) ->
                when (suppressionMode) {
                    NfcDispatchSuppressionMode.REDISPATCH -> {
                        disableHolderForegroundDispatch(nfcAdapter)
                        Napier.i("Suppressing NFC tag redispatch while suppression is active")
                        nfcAdapter.enableReaderMode(
                            this@AbstractWalletActivity,
                            { Napier.d("Suppressing NFC tag redispatch") },
                            NfcAdapter.FLAG_READER_NFC_A or
                                    NfcAdapter.FLAG_READER_NFC_B or
                                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or
                                    NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                            null
                        )
                    }

                    NfcDispatchSuppressionMode.DISABLED -> {
                        disableHolderForegroundDispatch(nfcAdapter)
                        Napier.i("NFC disabled while suppression is active")
                        withContext(Dispatchers.IO) {
                            nfcAdapter.disableReaderMode(this@AbstractWalletActivity)
                        }
                    }

                    NfcDispatchSuppressionMode.NONE -> {
                        if (holderTransfer && !verifierTransfer) {
                            Napier.i("Holder NFC data transfer active; disabling reader mode and capturing foreground tag dispatch")
                            withContext(Dispatchers.IO) {
                                nfcAdapter.disableReaderMode(this@AbstractWalletActivity)
                            }
                            enableHolderForegroundDispatch(nfcAdapter)
                        } else if (!verifierTransfer) {
                            disableHolderForegroundDispatch(nfcAdapter)
                            Napier.i("Verifier NFC tag redispatch suppression ended; disabling reader mode")
                            withContext(Dispatchers.IO) {
                                nfcAdapter.disableReaderMode(this@AbstractWalletActivity)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        preferredServiceJob?.cancel()
        preferredServiceJob = null
        verifierNfcJob?.cancel()
        verifierNfcJob = null
        verifierTagDispatchJob?.cancel()
        verifierTagDispatchJob = null
        NfcAdapter.getDefaultAdapter(this)?.let {
            disableHolderForegroundDispatch(it)
            val cardEmulation = CardEmulation.getInstance(it)
            if (NfcTransferState.nfcDataTransferActive.value &&
                !NfcTransferState.verifierNfcTransferActive.value &&
                NfcTransferState.verifierNfcTagDispatchSuppressed.value == NfcDispatchSuppressionMode.NONE
            ) {
                Napier.i(
                    "Activity paused during active NFC data transfer; keeping preferred NFC HCE service"
                )
            } else {
                Napier.i("Activity paused; unsetting preferred NFC HCE service")
                if (!cardEmulation.unsetPreferredService(this)) {
                    Napier.w("CardEmulation.unsetPreferredService() returned false")
                }
            }
            if (NfcTransferState.verifierNfcTransferActive.value ||
                NfcTransferState.verifierNfcTagDispatchSuppressed.value != NfcDispatchSuppressionMode.NONE
            ) {
                Napier.i(
                    "Activity paused during active verifier NFC transfer; keeping reader mode unchanged " +
                            "verifierActive=${NfcTransferState.verifierNfcTransferActive.value}, " +
                            "tagDispatchSuppressed=${NfcTransferState.verifierNfcTagDispatchSuppressed.value}"
                )
            } else {
                Napier.i("Activity paused; disabling reader mode")
                it.disableReaderMode(this)
            }
        }
    }

    private fun enableHolderForegroundDispatch(nfcAdapter: NfcAdapter) {
        if (holderForegroundDispatchEnabled) return
        registerHolderForegroundDispatchReceiver()
        val intent = Intent(ACTION_SUPPRESS_HOLDER_NFC_TAG).setPackage(packageName)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        Napier.i("Enabling holder foreground NFC dispatch during data transfer")
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null)
        holderForegroundDispatchEnabled = true
    }

    private fun disableHolderForegroundDispatch(nfcAdapter: NfcAdapter) {
        if (!holderForegroundDispatchEnabled) return
        Napier.i("Disabling holder foreground NFC dispatch")
        nfcAdapter.disableForegroundDispatch(this)
        holderForegroundDispatchEnabled = false
        unregisterHolderForegroundDispatchReceiver()
    }

    private fun registerHolderForegroundDispatchReceiver() {
        if (holderForegroundDispatchReceiverRegistered) return
        val filter = IntentFilter(ACTION_SUPPRESS_HOLDER_NFC_TAG)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(holderForegroundDispatchReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(holderForegroundDispatchReceiver, filter)
        }
        holderForegroundDispatchReceiverRegistered = true
    }

    private fun unregisterHolderForegroundDispatchReceiver() {
        if (!holderForegroundDispatchReceiverRegistered) return
        try {
            unregisterReceiver(holderForegroundDispatchReceiver)
        } catch (e: IllegalArgumentException) {
            Napier.w("Holder foreground NFC dispatch receiver was already unregistered", e)
        }
        holderForegroundDispatchReceiverRegistered = false
    }

    companion object {
        private const val ACTION_SUPPRESS_HOLDER_NFC_TAG =
            "at.asitplus.wallet.app.android.action.SUPPRESS_HOLDER_NFC_TAG"
    }
}
