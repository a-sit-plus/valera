package at.asitplus.wallet.app.common.data.primitives

import at.asitplus.wallet.app.common.domain.vck.tokenStatusList.StatusListTokenResolver
import at.asitplus.wallet.lib.data.StatusListToken
import at.asitplus.wallet.lib.data.rfc3986.UniformResourceIdentifier

/**
 * This class does not care about the underlying caching rules.
 */
data class CachingStatusListTokenResolver(
    val store: SimpleStore<UniformResourceIdentifier, StatusListToken>,
    val statusListTokenResolver: StatusListTokenResolver,
) : StatusListTokenResolver {
    override suspend fun invoke(uniformResourceIdentifier: UniformResourceIdentifier) =
        store.getOrPut(uniformResourceIdentifier) {
            statusListTokenResolver(uniformResourceIdentifier)
        }
}