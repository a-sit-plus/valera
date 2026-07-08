package at.asitplus.wallet.rp.registration.wrprc

import at.asitplus.wallet.app.common.relyingParty.data.WrpCredentialDto
import at.asitplus.wallet.app.common.relyingParty.data.WrpIntermediaryDto
import at.asitplus.wallet.app.common.relyingParty.data.WrpLangStringDto
import at.asitplus.wallet.app.common.relyingParty.data.WrpStatusDto
import at.asitplus.wallet.app.common.relyingParty.data.WrpSupervisoryAuthorityDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * WRPRC payload aligned to ETSI TS 119 475 V1.2.1 Annex C.
 */
@Serializable
data class WrpPayloadDto(
    @SerialName("name")
    val name: String? = null,

    @SerialName("sub_ln")
    val subLn: String? = null,

    @SerialName("sub_gn")
    val subGn: String? = null,

    @SerialName("sub_fn")
    val subFn: String? = null,

    @SerialName("sub")
    val sub: String,

    @SerialName("country")
    val country: String,

    @SerialName("registry_uri")
    val registryUri: String,

    @SerialName("srv_description")
    val srvDescription: List<List<WrpLangStringDto>>,

    @SerialName("entitlements")
    val entitlements: List<String>,

    @SerialName("privacy_policy")
    val privacyPolicy: String,

    @SerialName("info_uri")
    val infoUri: String,

    @SerialName("support_uri")
    val supportUri: String? = null,

    @SerialName("supervisory_authority")
    val supervisoryAuthority: WrpSupervisoryAuthorityDto? = null,

    @SerialName("policy_id")
    val policyId: List<String> = emptyList(),

    @SerialName("certificate_policy")
    val certificatePolicy: String,

    @SerialName("iat")
    val iat: Long,

    @SerialName("status")
    val status: WrpStatusDto,

    @SerialName("purpose")
    val purpose: List<WrpLangStringDto> = emptyList(),

    @SerialName("credentials")
    val credentials: List<WrpCredentialDto> = emptyList(),

    @SerialName("intended_use_id")
    val intendedUseId: String? = null,

    @SerialName("provides_attestations")
    val providesAttestations: List<WrpCredentialDto> = emptyList(),

    @SerialName("public_body")
    val publicBody: Boolean? = null,

    @SerialName("intermediary")
    val intermediary: WrpIntermediaryDto? = null,

    @SerialName("exp")
    val exp: Long,
)
