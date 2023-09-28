package at.asitplus.digitalid.wallet.data

import at.asitplus.wallet.lib.data.VerifiableCredentialJws
import at.asitplus.wallet.pupilid.PupilIdCredential
import at.asitplus.wallet.pupilid.PupilIdCredentialHolder
import data.IdHolderCredential

object IdHolderCredentialFactory {
    fun from(list: List<VerifiableCredentialJws?>): IdHolderCredential? {
        val mapped = list.filterNotNull()
        if (mapped.isEmpty())
            return null
        if (mapped.size == 1) {
            val credential = mapped.first()
            val sub = credential.vc.credentialSubject
            if (sub is PupilIdCredential)
                return PupilIdCredentialHolder(sub, credential.vc)
            return null
        }
        return null
    }

}
