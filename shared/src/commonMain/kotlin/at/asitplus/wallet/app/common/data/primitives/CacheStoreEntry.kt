package at.asitplus.wallet.app.common.data.primitives

import kotlin.time.Instant

data class CacheStoreEntry<Data>(
    val data: Data,
    val createdTime: Instant,
)