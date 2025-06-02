package at.asitplus.wallet.app.common.domain.vck.tokenStatusList

import at.asitplus.wallet.lib.data.StatusListToken
import at.asitplus.wallet.lib.data.rfc3986.UniformResourceIdentifier

fun interface StatusListTokenResolver {
    suspend operator fun invoke(uniformResourceIdentifier: UniformResourceIdentifier): StatusListToken
}