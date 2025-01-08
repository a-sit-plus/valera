package at.asitplus.wallet.app.android.dcapi

import android.content.Intent

// Wrapper class for transporting received intents from the MainActivity in the androidApp module to the androidMain module
// similar workaround as appLink variable but suitable for complex objects
data class WalletAPIData(var intent: Intent? = null)
