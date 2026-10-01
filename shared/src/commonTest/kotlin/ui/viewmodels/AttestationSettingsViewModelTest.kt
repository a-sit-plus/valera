package ui.viewmodels

import at.asitplus.wallet.app.common.Configuration
import at.asitplus.wallet.app.common.ErrorService
import at.asitplus.wallet.app.common.WalletConfig
import data.storage.DummyDataStoreService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class AttestationSettingsViewModelTest {
    @Test
    fun disablingRemovesPersistedAttestationsAndResetsSessionCache() = runTest {
        val dataStore = DummyDataStoreService()
        val settings = WalletConfig(dataStore, ErrorService(this))
        val attestationKey = Configuration.DATASTORE_KEY_PROVISIONING_INSTANCE_ATTESTATION_BY_STATE
        dataStore.setPreference("stored statements", attestationKey)
        var resets = 0

        updateWalletProviderAttestation(false, settings, dataStore) { resets++ }

        assertFalse(settings.walletProviderAttestationEnabled.first())
        assertNull(dataStore.getPreference(attestationKey).first())
        assertEquals(1, resets)
    }

    @Test
    fun enablingKeepsStoredAttestations() = runTest {
        val dataStore = DummyDataStoreService()
        val settings = WalletConfig(dataStore, ErrorService(this))
        val attestationKey = Configuration.DATASTORE_KEY_PROVISIONING_INSTANCE_ATTESTATION_BY_STATE
        dataStore.setPreference("stored statements", attestationKey)
        var resets = 0

        updateWalletProviderAttestation(true, settings, dataStore) { resets++ }

        assertEquals("stored statements", dataStore.getPreference(attestationKey).first())
        assertEquals(0, resets)
    }
}
