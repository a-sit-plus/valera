package at.asitplus.wallet.app.android.dcapi

import android.content.Intent
import at.asitplus.wallet.app.common.dcapi.DCAPIInvocationData


data class DCAPIInvocationData(var intent: Intent, val sendCredentialResponseToInvoker: (String, Boolean) -> Unit) :
    DCAPIInvocationData
