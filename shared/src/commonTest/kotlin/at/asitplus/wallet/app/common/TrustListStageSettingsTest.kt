package at.asitplus.wallet.app.common

import at.asitplus.wallet.lib.etsi.LoteProfile
import at.asitplus.wallet.lib.etsi.LoteStage
import data.storage.DataStoreService
import data.storage.DummyDataStoreService
import data.storage.PersistentTrustListStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrustListStageSettingsTest {

    @Test
    fun enablesAcceptanceAndDevelopmentByDefault() = runTest {
        val walletConfig = walletConfig()

        assertEquals(
            setOf(LoteStage.ACCEPTANCE, LoteStage.DEVELOPMENT),
            walletConfig.trustListStages.first(),
        )
    }

    @Test
    fun togglesOneStageWithoutTouchingTheOthers() = runTest {
        val walletConfig = walletConfig()

        walletConfig.setTrustListStageEnabled(LoteStage.PRODUCTION, true).getOrThrow()
        walletConfig.setTrustListStageEnabled(LoteStage.ACCEPTANCE, false).getOrThrow()

        assertEquals(
            setOf(LoteStage.DEVELOPMENT, LoteStage.PRODUCTION),
            walletConfig.trustListStages.first(),
        )
    }

    @Test
    fun keepsAnEmptySelectionInsteadOfFallingBackToTheDefault() = runTest {
        val walletConfig = walletConfig()

        LoteStage.entries.forEach { walletConfig.setTrustListStageEnabled(it, false).getOrThrow() }

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
            setOf(LoteStage.DEVELOPMENT),
            walletConfig(dataStoreService).trustListStages.first(),
        )
    }

    @Test
    fun fetchesEveryListOfEveryEnabledStage() = runTest {
        val walletConfig = walletConfig()
        walletConfig.setTrustListStageEnabled(LoteStage.ACCEPTANCE, false).getOrThrow()

        val urls = LoteProfile.fetchUrls(walletConfig.trustListStages.first())

        assertEquals(LoteProfile.entries.size, urls.size)
        assertTrue(urls.contains("https://development.trust.tech.ec.europa.eu/lists/eudiw/pid-providers.json"))
        assertFalse(urls.any { it.contains("acceptance") })
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
