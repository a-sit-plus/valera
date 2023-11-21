package data.idaustria

import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.SchemaIndex.BASE

object ConstantIndex {

    object IdAustriaCredential : ConstantIndex.CredentialScheme {
        override val schemaUri: String = "$BASE/schemas/1.0.0/idaustria.json"
        override val vcType: String = "IdAustria2023"
        override val isoNamespace: String = "at.gv.id-austria.2023"
        override val isoDocType: String = "at.gv.id-austria.2023.iso"
    }

}