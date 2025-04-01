package at.asitplus.wallet.app.android

import android.content.ComponentName
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.identitycredentials.GetCredentialResponse
import com.google.android.gms.identitycredentials.IntentHelper
import io.github.aakira.napier.Napier
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.multipaz.context.initializeApplication
import java.security.Security

abstract class AbstractWalletActivity : AppCompatActivity() {

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

        initializeApplication(this.applicationContext)
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
                Napier.w("CardEmulation.unsetPreferredService() return false")
            }
        }
    }
}