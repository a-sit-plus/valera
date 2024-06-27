package at.asitplus.wallet.app.common.third_party.at.asitplus.wallet.lib.data

import at.asitplus.wallet.lib.data.AttributeIndex
import at.asitplus.wallet.lib.data.ConstantIndex

val ConstantIndex.CredentialScheme.identifier: String
    get() = vcType ?: isoDocType ?: sdJwtType ?: schemaUri