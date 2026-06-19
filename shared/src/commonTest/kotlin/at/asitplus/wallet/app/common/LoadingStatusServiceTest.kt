package at.asitplus.wallet.app.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LoadingStatusServiceTest {
    @Test
    fun setAndClearUpdatesMessage() {
        val service = LoadingStatusService()

        assertNull(service.message.value)

        service.set(LoadingMessageKey.IssuingCredential)
        assertEquals(LoadingMessageKey.IssuingCredential, service.message.value)

        service.clear()
        assertNull(service.message.value)
    }
}
