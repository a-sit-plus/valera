package ui.navigation

import at.asitplus.wallet.app.common.DummyPlatformAdapter
import kotlin.test.Test
import kotlin.test.assertEquals

class IntentServiceTest {
    private val intentService = IntentService(DummyPlatformAdapter())

    @Test
    fun `parseUrl recognizes initial signing request`() {
        val url = "eudi-rqes://createSignRequest?request_uri=https%3A%2F%2Fexample.com%2Freq"

        assertEquals(IntentService.IntentType.SigningIntent, intentService.parseUrl(url))
    }

    @Test
    fun `parseUrl recognizes signing callback`() {
        val url = "${IntentService.SIGNING_CALLBACK_URI}?code=abc&state=xyz"

        assertEquals(IntentService.IntentType.SigningResumeIntent, intentService.parseUrl(url))
    }

    @Test
    fun `parseUrl recognizes provisioning start request`() {
        val url = "openid-credential-offer://?credential_offer_uri=https%3A%2F%2Fissuer.example%2Foffer"

        assertEquals(IntentService.IntentType.ProvisioningStartIntent, intentService.parseUrl(url))
    }

    @Test
    fun `parseUrl recognizes provisioning callback`() {
        val url = "${IntentService.PROVISIONING_CALLBACK_URI}?code=abc&state=xyz"

        assertEquals(IntentService.IntentType.ProvisioningResumeIntent, intentService.parseUrl(url))
    }

    @Test
    fun `parseUrl prefers explicit signing callback over generic error detection`() {
        val url = "${IntentService.SIGNING_CALLBACK_URI}?error=access_denied"

        assertEquals(IntentService.IntentType.SigningResumeIntent, intentService.parseUrl(url))
    }
}
