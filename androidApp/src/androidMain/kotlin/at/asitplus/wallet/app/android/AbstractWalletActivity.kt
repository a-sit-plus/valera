package at.asitplus.wallet.app.android

import android.content.ComponentName
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.android.identity.util.AndroidContexts
import com.google.android.gms.identitycredentials.GetCredentialResponse
import com.google.android.gms.identitycredentials.IntentHelper
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

abstract class AbstractWalletActivity : AppCompatActivity()  {

    abstract fun populateLink(intent: Intent)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        populateLink(intent)
        initIdentityLibraries()
    }

    private fun initIdentityLibraries() {
        // required for identity.Crypto classes
        Security.removeProvider("BC")
        Security.addProvider(BouncyCastleProvider())

        AndroidContexts.setApplicationContext(applicationContext)
        AndroidContexts.setCurrentActivity(this)
    }

    fun sendCredentialResponseToDCAPIInvoker(resultJson: String) {
        val resultData = Intent()
        val bundle = Bundle().apply {
            putByteArray("identityToken", resultJson.toByteArray())
        }
        val credentialResponse = com.google.android.gms.identitycredentials.Credential("type", bundle)

        IntentHelper.setGetCredentialResponse(
            resultData,
            GetCredentialResponse(credentialResponse)
        )
        setResult(RESULT_OK, resultData)
        finish()
    }

    override fun onResume() {
        super.onResume()
        AndroidContexts.setCurrentActivity(this)
        NfcAdapter.getDefaultAdapter(this)?.let {
            CardEmulation.getInstance(it)?.setPreferredService(this, ComponentName(this, NdefDeviceEngagementService::class::class.java))
        }
    }

    override fun onPause() {
        super.onPause()
        AndroidContexts.setCurrentActivity(null)
        NfcAdapter.getDefaultAdapter(this)?.let {
            CardEmulation.getInstance(it)?.unsetPreferredService(this)
        }
    }
}