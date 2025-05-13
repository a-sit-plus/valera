package at.asitplus.wallet.app.data

import at.asitplus.wallet.lib.agent.validation.StatusListTokenResolver
import at.asitplus.wallet.lib.data.StatusListToken
import at.asitplus.wallet.lib.data.rfc3986.UniformResourceIdentifier

/**
 * This class does not care about the underlying caching rules.
 */
data class CachingStatusListTokenResolver(
    val store: SimpleStore<UniformResourceIdentifier, StatusListToken>,
    val statusListTokenResolver: StatusListTokenResolver,
) : StatusListTokenResolver {
    override suspend fun invoke(statusListUrl: UniformResourceIdentifier) =
        store.getOrPut(statusListUrl) {
            statusListTokenResolver(statusListUrl)
        }
}