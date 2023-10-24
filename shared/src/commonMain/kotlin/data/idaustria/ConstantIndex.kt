package data.idaustria

import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.SchemaIndex.BASE

object ConstantIndex {
    object IdAustriaCredential : ConstantIndex.CredentialScheme {
        override val credentialDefinitionName: String = "idaustria"
        override val schemaUri: String = "$BASE/schemas/1.0.0/idaustria.json"
        override val vcType: String = "IdAustria2023"
        override val credentialFormat: ConstantIndex.CredentialFormat = ConstantIndex.CredentialFormat.W3C_VC
    }
}

