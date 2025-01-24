package data.bletransfer.holder

import android.content.Context
import android.security.identity.IdentityCredentialStore

class CredentialStore(
    private val context: Context
) {

    fun createIdentityCredentialStore(): IdentityCredentialStore? {
        return IdentityCredentialStore.getInstance(context)
//        return if (PreferencesHelper.isHardwareBacked())
//            IdentityCredentialStore.getHardwareInstance(context)
//                ?: createKeystoreBackedStore() else createKeystoreBackedStore()
    }

//    private fun createKeystoreBackedStore(): IdentityCredentialStore {
//        val keystoreBackedStorageLocation = PreferencesHelper
//            .getKeystoreBackedStorageLocation(context)
//        return IdentityCredentialStore
//            .getKeystoreInstance(context, keystoreBackedStorageLocation)
//    }
}