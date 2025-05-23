package at.asitplus.wallet.app.common.data.primitives

import kotlinx.datetime.Instant

data class CacheStoreEntry<Data>(
    val data: Data,
    val createdTime: Instant,
)