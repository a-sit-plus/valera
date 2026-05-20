package at.asitplus.wallet.app.android

import android.content.ComponentName
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CreateDigitalCredentialResponse
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.credentials.DigitalCredential
import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.CreateCredentialCustomException
import androidx.credentials.exceptions.GetCredentialCustomException
import androidx.credentials.provider.PendingIntentHandler
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.wallet.app.common.dcapi.data.ErrorResponse
import io.github.aakira.napier.Napier
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.multipaz.context.initializeApplication
import java.security.Security

abstract class AbstractWalletActivity : AppCompatActivity() {

    abstract fun populateLink(intent: Intent)

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

    override fun onResume() {
        super.onResume()
        val nfcAdapter = NfcAdapter.getDefaultAdapter(this) ?: return
        val cardEmulation = CardEmulation.getInstance(nfcAdapter)
        if (!cardEmulation.categoryAllowsForegroundPreference(CardEmulation.CATEGORY_OTHER)) {
            Napier.w("CardEmulation.categoryAllowsForegroundPreference(CATEGORY_OTHER) returned false")
        }
        preferredServiceJob = lifecycleScope.launch {
            NdefDeviceEngagementService.nfcDataTransferActive.collect { dataTransfer ->
                val serviceClass = if (dataTransfer) NfcDataRetrievalService::class.java
                                   else NdefDeviceEngagementService::class.java
                if (!cardEmulation.setPreferredService(this@AbstractWalletActivity, ComponentName(this@AbstractWalletActivity, serviceClass))) {
                    Napier.w("CardEmulation.setPreferredService() returned false for ${serviceClass.simpleName}")
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        preferredServiceJob?.cancel()
        preferredServiceJob = null
        NfcAdapter.getDefaultAdapter(this)?.let {
            val cardEmulation = CardEmulation.getInstance(it)
            if (!cardEmulation.unsetPreferredService(this)) {
                Napier.w("CardEmulation.unsetPreferredService() returned false")
            }
        }
    }
}