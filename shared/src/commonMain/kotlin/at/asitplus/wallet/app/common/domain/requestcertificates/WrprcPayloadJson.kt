package at.asitplus.wallet.app.common.domain.requestcertificates

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal object WrprcPayloadClaims {
    const val Issuer = "iss"
    const val Subject = "sub"
    const val Name = "name"
    const val RegistryUri = "registry_uri"
    const val IntendedUseId = "intended_use_id"
    const val PrivacyPolicy = "privacy_policy"
    const val ServiceDescription = "srv_description"
    const val Credentials = "credentials"
    const val IssuedAt = "iat"
    const val Expiration = "exp"
}

internal fun JsonObject.stringField(key: String): String? =
    (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

internal fun JsonObject.longField(key: String): Long? =
    (this[key] as? JsonPrimitive)?.contentOrNull?.toLongOrNull()

internal fun JsonObject.hasNonEmptyArray(key: String): Boolean =
    (this[key] as? JsonArray)?.any { it.isMeaningfullyPresent() } == true

internal fun JsonElement.isMeaningfullyPresent(): Boolean = when (this) {
    is JsonPrimitive -> !contentOrNull.isNullOrBlank()
    is JsonArray -> any { it.isMeaningfullyPresent() }
    is JsonObject -> values.any { it.isMeaningfullyPresent() }
}

