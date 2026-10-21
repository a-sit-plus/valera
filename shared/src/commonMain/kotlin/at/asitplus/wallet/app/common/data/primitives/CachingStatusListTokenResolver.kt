package at.asitplus.wallet.app.common.data.primitives

import at.asitplus.wallet.lib.agent.validation.StatusListTokenResolver
import at.asitplus.wallet.lib.data.StatusListToken
import at.asitplus.wallet.lib.data.rfc3986.UniformResourceIdentifier

/**
 * This class does not care about the underlying caching rules.
 */
data class CachingStatusListTokenResolver(
    val store: SimpleStore<UniformResourceIdentifier, StatusListToken>,
    val statusListTokenResolver: StatusListTokenResolver,
    /** Resolves a status list token for [fresh], bypassing every cache, by default [statusListTokenResolver]. */
    val freshStatusListTokenResolver: StatusListTokenResolver = statusListTokenResolver,
) : StatusListTokenResolver {
    override suspend fun invoke(statusListUrl: UniformResourceIdentifier) =
        store.getOrPut(statusListUrl) {
            statusListTokenResolver(statusListUrl)
        }

    /**
     * Always resolves the status list token again with [freshStatusListTokenResolver], e.g. for the status of a
     * credential just received, and caches it in place of the cached one. The cached token is dropped before, so that
     * it is not used any more even if resolving fails.
     */
    val fresh = StatusListTokenResolver { statusListUrl ->
        store.remove(statusListUrl)
        freshStatusListTokenResolver(statusListUrl).also { store[statusListUrl] = it }
    }
}
