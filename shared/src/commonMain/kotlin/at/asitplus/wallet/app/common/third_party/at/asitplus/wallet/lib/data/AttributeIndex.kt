package at.asitplus.wallet.app.common.third_party.at.asitplus.wallet.lib.data

import at.asitplus.wallet.lib.data.AttributeIndex

fun AttributeIndex.resolveSchemeByIdentifier(schemeIdentifier: String) =
    resolveAttributeType(schemeIdentifier) ?: resolveIsoDoctype(schemeIdentifier)
    ?: resolveSchemaUri(schemeIdentifier) ?: resolveSdJwtAttributeType(schemeIdentifier)