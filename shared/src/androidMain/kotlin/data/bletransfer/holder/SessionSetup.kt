package data.bletransfer.holder

import com.android.identity.android.legacy.IdentityCredentialStore.CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256
import com.android.identity.android.legacy.PresentationSession

class SessionSetup(
    private val credentialStore: CredentialStore
) {

    fun createSession(): PresentationSession {
        val store = credentialStore.createIdentityCredentialStore()
        return store.createPresentationSession(
            CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256
        )
    }
}

//import android.security.identity.IdentityCredentialStore
//import android.security.identity.IdentityCredentialStore.CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256
//import android.security.identity.PresentationSession
//
//class SessionSetup(
//    private val credentialStore: IdentityCredentialStore
//) {
//
//    fun createSession(): PresentationSession {
//        return credentialStore.createPresentationSession(
//            CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256
//        )
//    }
//}
