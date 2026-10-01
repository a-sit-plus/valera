package at.asitplus.wallet.app.common.presentation

import at.asitplus.wallet.lib.zk.iso.IsoMdocZkBackendRegistry
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LongfellowZkRegistrationTest {
    @Test
    fun registersLongfellowBackendOnce() = runTest {
        LongfellowZkRegistration.ensureRegistered()
        LongfellowZkRegistration.ensureRegistered()

        val backends = IsoMdocZkBackendRegistry.Default.backends.filter { it.system.startsWith("longfellow") }
        assertEquals(1, backends.size)
    }
}
