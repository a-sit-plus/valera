package ui.composables

import at.asitplus.KmmResult
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.primitives.TokenStatus

sealed interface CredentialStatusState {
    data object Loading : CredentialStatusState
    data class Success(
        val tokenStatus: KmmResult<TokenStatus?>,
    ) : CredentialStatusState
}