package at.asitplus.wallet.app.common

import at.asitplus.wallet.lib.agent.FixedTimeClock
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation.SD_JWT
import at.asitplus.wallet.lib.data.CredentialMetadataRegistry
import at.asitplus.wallet.lib.data.ResolvedCredentialMetadata
import at.asitplus.wallet.sdjwt.SdJwtTypeMetadata
import at.asitplus.wallet.sdjwt.SdJwtVcType
import data.storage.DummyDataStoreService
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days

class PersistentCachingCredentialMetadataRegistryTest {
    @Test
    fun returnsStaleMetadataWhenRefreshFails() = runTest {
        val dataStore = DummyDataStoreService()
        val cachedAt = 1_000_000L
        val resolved = ResolvedCredentialMetadata(
            metadata = SdJwtTypeMetadata(SdJwtVcType("test"), name = "Cached metadata"),
            loadedFrom = "https://example.test/metadata.json",
        )

        val initial = PersistentCachingCredentialMetadataRegistry(
            delegate = registryReturning(resolved),
            dataStore = dataStore,
            clock = FixedTimeClock(cachedAt),
        )
        assertEquals(resolved, initial.findEntry("test", SD_JWT))

        var prematureRefreshes = 0
        val fresh = PersistentCachingCredentialMetadataRegistry(
            delegate = object : CredentialMetadataRegistry {
                override suspend fun findEntry(
                    identifier: String,
                    representation: CredentialRepresentation,
                ): ResolvedCredentialMetadata? {
                    prematureRefreshes++
                    return null
                }
            },
            dataStore = dataStore,
            clock = FixedTimeClock(cachedAt + 1.days.inWholeMilliseconds),
        )
        assertEquals(resolved, fresh.findEntry("test", SD_JWT))
        assertEquals(0, prematureRefreshes)

        var refreshes = 0
        val offline = PersistentCachingCredentialMetadataRegistry(
            delegate = object : CredentialMetadataRegistry {
                override suspend fun findEntry(
                    identifier: String,
                    representation: CredentialRepresentation,
                ): ResolvedCredentialMetadata? {
                    refreshes++
                    return null
                }
            },
            dataStore = dataStore,
            clock = FixedTimeClock(cachedAt + 8.days.inWholeMilliseconds),
        )

        assertEquals(resolved, offline.findEntry("test", SD_JWT))
        assertEquals(1, refreshes)
    }

    private fun registryReturning(resolved: ResolvedCredentialMetadata) = object : CredentialMetadataRegistry {
        override suspend fun findEntry(
            identifier: String,
            representation: CredentialRepresentation,
        ) = resolved
    }
}
