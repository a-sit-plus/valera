package at.asitplus.wallet.app

import at.asitplus.wallet.app.common.ErrorService
import at.asitplus.wallet.app.common.BuildType
import at.asitplus.wallet.app.common.WalletConfig
import at.asitplus.wallet.app.common.Configuration
import data.storage.DummyDataStoreService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommonUnitTests {
    @Test
    fun testDataStoreService() {
        val dataStoreService = DummyDataStoreService()
        runBlocking { dataStoreService.setPreference(value = Configuration.DEBUG_DATASTORE_VALUE, key = Configuration.DEBUG_DATASTORE_KEY) }
        val result = runBlocking { dataStoreService.getPreference(key = Configuration.DEBUG_DATASTORE_KEY).first() }
        assertEquals(Configuration.DEBUG_DATASTORE_VALUE, result)
    }

    @Test
    fun testDataStoreServiceFlow() {
        val dataStoreService = DummyDataStoreService()

        val delegate = dataStoreService.getPreference(Configuration.DEBUG_DATASTORE_KEY)

        runBlocking {
            assertEquals(null, delegate.firstOrNull())
        }
        runBlocking {
            dataStoreService.setPreference(value = "0", key = Configuration.DEBUG_DATASTORE_KEY)
        }
        runBlocking {
            assertEquals("0", delegate.firstOrNull())
        }
        runBlocking {
            dataStoreService.setPreference(value = "1", key = Configuration.DEBUG_DATASTORE_KEY)
        }
        runBlocking {
            assertEquals("1", delegate.firstOrNull())
        }
    }

    @Test
    fun testBleToggleRestoresPreviousModeSelection() = runTest {
        val walletConfig = WalletConfig(
            dataStoreService = DummyDataStoreService(),
            errorService = ErrorService(this)
        )

        walletConfig.setPresentmentBlePeripheralServerModeEnabled(false).getOrThrow()
        walletConfig.setPresentmentBleEnabled(false).getOrThrow()

        assertEquals(false, walletConfig.presentmentBleCentralClientModeEnabled.first())
        assertEquals(false, walletConfig.presentmentBlePeripheralServerModeEnabled.first())

        walletConfig.setPresentmentBleEnabled(true).getOrThrow()

        assertEquals(true, walletConfig.presentmentBleCentralClientModeEnabled.first())
        assertEquals(false, walletConfig.presentmentBlePeripheralServerModeEnabled.first())
    }

    @Test
    fun testBleToggleFallsBackToDefaultWhenBothModesWereTurnedOff() = runTest {
        val walletConfig = WalletConfig(
            dataStoreService = DummyDataStoreService(),
            errorService = ErrorService(this)
        )

        walletConfig.setPresentmentBleCentralClientModeEnabled(false).getOrThrow()
        walletConfig.setPresentmentBlePeripheralServerModeEnabled(false).getOrThrow()

        assertEquals(false, walletConfig.presentmentBleCentralClientModeEnabled.first())
        assertEquals(false, walletConfig.presentmentBlePeripheralServerModeEnabled.first())

        walletConfig.setPresentmentBleEnabled(true).getOrThrow()

        assertEquals(true, walletConfig.presentmentBleCentralClientModeEnabled.first())
        assertEquals(true, walletConfig.presentmentBlePeripheralServerModeEnabled.first())
    }

    @Test
    fun testOpenId4VpOriginSchemeDefaultsDependOnBuildType() = runTest {
        val releaseConfig = WalletConfig(
            dataStoreService = DummyDataStoreService(),
            errorService = ErrorService(this),
            buildType = BuildType.RELEASE,
        )
        val debugConfig = WalletConfig(
            dataStoreService = DummyDataStoreService(),
            errorService = ErrorService(this),
            buildType = BuildType.DEBUG,
        )

        assertEquals(
            setOf("https", "android:apk-key-hash"),
            releaseConfig.openId4VpAllowedOriginSchemes.first(),
        )
        assertFalse("http" in releaseConfig.openId4VpAllowedOriginSchemes.first())
        assertTrue("http" in debugConfig.openId4VpAllowedOriginSchemes.first())
    }

    @Test
    fun testOpenId4VpOriginSchemesCanReplaceOrDisableDefaults() = runTest {
        val walletConfig = WalletConfig(
            dataStoreService = DummyDataStoreService(),
            errorService = ErrorService(this),
            buildType = BuildType.DEBUG,
        )

        walletConfig.set(openId4VpAllowedOriginSchemes = setOf("https", "custom")).getOrThrow()
        assertEquals(setOf("https", "custom"), walletConfig.openId4VpAllowedOriginSchemes.first())

        walletConfig.set(openId4VpAllowedOriginSchemes = emptySet()).getOrThrow()
        assertEquals(emptySet(), walletConfig.openId4VpAllowedOriginSchemes.first())
    }
}
