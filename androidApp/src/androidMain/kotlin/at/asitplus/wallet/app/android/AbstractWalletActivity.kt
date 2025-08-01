package at.asitplus.wallet.app.android

import android.content.ComponentName
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.DigitalCredential
import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.GetCredentialResponse
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
            sendErrorResponse(resultStr, resultData)
        }

        setResult(RESULT_OK, resultData)
        finish()
    }

    private fun sendErrorResponse(resultStr: String, resultData: Intent) {
        /* TODO check with SP that supports exceptions whether this works
              * otherwise try with the Google GMS library (see above)
            */
        Napier.v("Returning error response: $resultStr")
        PendingIntentHandler.setGetCredentialException(
            resultData,
            GetCredentialCustomException(
                resultStr, resultStr
            )
        )
    }

    override fun onResume() {
        super.onResume()
        NfcAdapter.getDefaultAdapter(this)?.let {
            val cardEmulation = CardEmulation.getInstance(it)
            if (!cardEmulation.setPreferredService(this, ComponentName(this, NdefDeviceEngagementService::class.java))) {
                Napier.w("CardEmulation.setPreferredService() returned false")
            }
            if (!cardEmulation.categoryAllowsForegroundPreference(CardEmulation.CATEGORY_OTHER)) {
                Napier.w("CardEmulation.categoryAllowsForegroundPreference(CATEGORY_OTHER) returned false")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        NfcAdapter.getDefaultAdapter(this)?.let {
            val cardEmulation = CardEmulation.getInstance(it)
            if (!cardEmulation.unsetPreferredService(this)) {
                Napier.w("CardEmulation.unsetPreferredService() returned false")
            }
        }
    }
}