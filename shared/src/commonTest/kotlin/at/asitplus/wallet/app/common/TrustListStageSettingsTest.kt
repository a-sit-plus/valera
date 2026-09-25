package at.asitplus.wallet.app.common

import at.asitplus.wallet.lib.etsi.LoteProfile
import at.asitplus.wallet.lib.etsi.LoTEStage
import data.storage.DataStoreService
import data.storage.DummyDataStoreService
import data.storage.PersistentTrustListStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class TrustListStageSettingsTest {

    @Test
    fun enablesAcceptanceAndDevelopmentByDefault() = runTest {
        val walletConfig = walletConfig()

        assertEquals(
            setOf(LoTEStage.ACCEPTANCE, LoTEStage.DEVELOPMENT),
            walletConfig.trustListStages.first(),
        )
    }

    @Test
    fun togglesOneStageWithoutTouchingTheOthers() = runTest {
        val walletConfig = walletConfig()

        walletConfig.setTrustListStageEnabled(LoTEStage.PRODUCTION, true).getOrThrow()
        walletConfig.setTrustListStageEnabled(LoTEStage.ACCEPTANCE, false).getOrThrow()

        assertEquals(
            setOf(LoTEStage.DEVELOPMENT, LoTEStage.PRODUCTION),
            walletConfig.trustListStages.first(),
        )
    }

    @Test
    fun keepsAnEmptySelectionInsteadOfFallingBackToTheDefault() = runTest {
        val walletConfig = walletConfig()

        LoTEStage.entries.forEach { walletConfig.setTrustListStageEnabled(it, false).getOrThrow() }

        assertEquals(emptySet(), walletConfig.trustListStages.first())
        assertEquals(emptyList(), LoteProfile.fetchUrls(walletConfig.trustListStages.first()))
    }

    @Test
    fun ignoresStoredStagesThisVersionDoesNotKnow() = runTest {
        val dataStoreService = DummyDataStoreService()
        dataStoreService.setPreference(
            """{"trustListStages":["DEVELOPMENT","SOME_FUTURE_STAGE"]}""",
            Configuration.DATASTORE_KEY_CONFIG,
        )

        assertEquals(
            setOf(LoTEStage.DEVELOPMENT),
            walletConfig(dataStoreService).trustListStages.first(),
        )
    }

    @Test
    fun fetchesEveryListOfEveryEnabledStage() = runTest {
        val walletConfig = walletConfig()
        walletConfig.setTrustListStageEnabled(LoTEStage.ACCEPTANCE, false).getOrThrow()

        val urls = LoteProfile.fetchUrls(walletConfig.trustListStages.first())

        assertEquals(LoteProfile.entries.size, urls.size)
        assertTrue(urls.contains("https://development.trust.tech.ec.europa.eu/lists/eudiw/pid-providers.json"))
        assertFalse(urls.any { it.contains("acceptance") })
    }

    @Test
    fun prunesEveryListOfEveryDisabledStage() {
        val enabled = LoTEStage.ACCEPTANCE.fetchUrls

        val pruned = disabledTrustListUrls(enabled)

        assertEquals(LoteProfile.entries.size * (LoTEStage.entries.size - 1), pruned.size)
        assertTrue(pruned.containsAll(LoTEStage.DEVELOPMENT.fetchUrls))
        assertTrue(pruned.none { it in enabled })
    }

    @Test
    fun removesTheCachedListOfADisabledStage() = runTest {
        val dataStoreService = DummyDataStoreService()
        val store = PersistentTrustListStore(dataStoreService)
        val url = LoTEStage.PRODUCTION.fetchUrl(LoteProfile.PID)
        store.persistTrustList(url, "cached-trust-list", Instant.fromEpochMilliseconds(1_000))

        assertTrue(store.removeTrustList(url))

        assertNull(dataStoreService.getPreference(url).first())
        assertFalse(store.removeTrustList(url))
    }

    @Test
    fun concurrentRemovalsReportOnlyOneRemoval() = runTest {
        val backing = DummyDataStoreService()
        val url = LoTEStage.PRODUCTION.fetchUrl(LoteProfile.PID)
        val bothRead = CompletableDeferred<Unit>()
        var reads = 0
        val dataStore = object : DataStoreService by backing {
            override fun getPreference(key: String): Flow<String?> = flow {
                val value = backing.getPreference(key).first()
                if (++reads == 2) bothRead.complete(Unit)
                bothRead.await()
                emit(value)
            }
        }
        val store = PersistentTrustListStore(dataStore)
        store.persistTrustList(url, "cached-trust-list", Instant.fromEpochMilliseconds(1_000))

        val results = listOf(
            async { store.removeTrustList(url) },
            async { store.removeTrustList(url) },
        ).awaitAll()

        assertEquals(listOf(false, true), results.sorted())
        assertNull(backing.getPreference(url).first())
    }

    @Test
    fun observesAnEmptyTrustContainerWithoutAnyEnabledStage() = runTest {
        val store = PersistentTrustListStore(DummyDataStoreService())

        assertEquals(emptyMap(), store.observeTrustContainer(emptyList()).first())
    }
}

private fun CoroutineScope.walletConfig(
    dataStoreService: DataStoreService = DummyDataStoreService(),
) = WalletConfig(
    dataStoreService = dataStoreService,
    errorService = ErrorService(this),
)
