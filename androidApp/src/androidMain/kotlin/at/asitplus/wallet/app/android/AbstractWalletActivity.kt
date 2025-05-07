package at.asitplus.wallet.app.android

import android.content.ComponentName
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CustomCredential
import androidx.credentials.DigitalCredential
import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.GetCredentialResponse
import androidx.credentials.provider.PendingIntentHandler
import io.github.aakira.napier.Napier
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.multipaz.context.initializeApplication
import java.security.Security

abstract class AbstractWalletActivity : AppCompatActivity() {

    abstract fun populateLink(intent: Intent)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    fun sendCredentialResponseToDCAPIInvoker(resultJson: String) {
        val resultData = Intent()
        /*val bundle = Bundle().apply {
            putByteArray("identityToken", resultJson.toByteArray())
        }

        val credentialResponse = com.google.android.gms.identitycredentials.Credential("type", bundle)

        IntentHelper.setGetCredentialResponse(
            resultData,
            GetCredentialResponse(credentialResponse)
        )
        */

        val credential = try {
            DigitalCredential(resultJson)
        } catch (e: IllegalArgumentException) {
            val bundle = Bundle().apply {
                putByteArray("identityToken", resultJson.toByteArray())
            }
            CustomCredential("type", bundle)
        }

        PendingIntentHandler.setGetCredentialResponse(
            resultData,
            GetCredentialResponse(
                credential
            )
        )
        setResult(RESULT_OK, resultData)
        finish()
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