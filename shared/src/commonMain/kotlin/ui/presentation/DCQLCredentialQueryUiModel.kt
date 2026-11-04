package ui.presentation

import at.asitplus.wallet.app.common.ZkMode

data class DCQLCredentialQueryUiModel(
    val credentialRepresentationLocalized: String?,
    val credentialSchemeLocalized: String,
    val requestedAttributesLocalized: DCQLCredentialQueryUiModelAttributeLabels?,
    val zkMode: ZkMode = ZkMode.UNKNOWN,
)

