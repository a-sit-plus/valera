package at.asitplus.wallet.app.common.domain.requestcertificates

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * DTOs for the registrar's signed public /wrp endpoints.
 *
 * These types are local to Valera for now. If the same public registrar contract
 * is consumed by multiple clients, they should move into a shared library later.
 */
@Serializable
internal data class RegistrarPublicServiceDto(
    @SerialName("serviceUri")
    val serviceUri: String,

    @SerialName("displayName")
    val displayName: String? = null,

    @SerialName("intendedUse")
    val intendedUse: List<RegistrarIntendedUseDto> = emptyList(),
)

@Serializable
internal data class RegistrarRegistrationInfoDto(
    @SerialName("wrpIdentifier")
    val wrpIdentifier: String,

    @SerialName("serviceUri")
    val serviceUri: String,

    @SerialName("displayName")
    val displayName: String? = null,

    @SerialName("intendedUses")
    val intendedUses: List<RegistrarIntendedUseDto> = emptyList(),
)

@Serializable
internal data class RegistrarIntendedUseDto(
    @SerialName("intendedUseIdentifier")
    val intendedUseIdentifier: String? = null,

    @SerialName("purpose")
    val purpose: JsonElement? = null,

    @SerialName("privacyPolicy")
    val privacyPolicy: JsonElement? = null,

    @SerialName("credentials")
    val credentials: JsonElement? = null,

    @SerialName("createdAt")
    val createdAt: String? = null,

    @SerialName("revokedAt")
    val revokedAt: String? = null,
)
