package ui.viewmodels

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QrCodeScannerViewModelTest {

    @Test
    fun recognizesFidoCrossDeviceQrCodesCaseInsensitively() {
        assertTrue("FIDO:/123456789".isFidoCrossDeviceQrCode())
        assertTrue("fido:/123456789".isFidoCrossDeviceQrCode())
    }

    @Test
    fun doesNotTreatOtherQrCodePayloadsAsCrossDeviceQrCodes() {
        assertFalse("openid4vp://authorize?request=example".isFidoCrossDeviceQrCode())
        assertFalse("FIDO://123456789".isFidoCrossDeviceQrCode())
        assertFalse(" FIDO:/123456789".isFidoCrossDeviceQrCode())
    }
}
