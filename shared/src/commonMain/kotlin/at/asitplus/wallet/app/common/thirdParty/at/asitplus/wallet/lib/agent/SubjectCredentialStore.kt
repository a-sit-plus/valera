package at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.agent

import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex

val SubjectCredentialStore.StoreEntry.representation: ConstantIndex.CredentialRepresentation
    get() = when (this) {
        is SubjectCredentialStore.StoreEntry.Vc -> ConstantIndex.CredentialRepresentation.PLAIN_JWT
        is SubjectCredentialStore.StoreEntry.SdJwt -> ConstantIndex.CredentialRepresentation.SD_JWT
        is SubjectCredentialStore.StoreEntry.Iso -> ConstantIndex.CredentialRepresentation.ISO_MDOC
    }
