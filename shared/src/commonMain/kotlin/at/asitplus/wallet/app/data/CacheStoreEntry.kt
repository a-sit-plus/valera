package at.asitplus.wallet.app.data

import kotlinx.datetime.Instant

data class CacheStoreEntry<Data>(
    val data: Data,
    val createdTime: Instant,
)