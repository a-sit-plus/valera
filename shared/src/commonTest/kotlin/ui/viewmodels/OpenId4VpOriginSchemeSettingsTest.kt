package ui.viewmodels

import kotlin.test.Test
import kotlin.test.assertEquals

class OpenId4VpOriginSchemeSettingsTest {
    @Test
    fun ignoresWhitespaceAndDuplicateCommas() {
        assertEquals(
            setOf("https", "android:apk-key-hash"),
            " https,,, android: apk-key-hash,, ".toOriginSchemeSet(),
        )
    }

    @Test
    fun formatsSchemesWithoutSpacesOrDuplicateCommas() {
        assertEquals(
            "android:apk-key-hash,https",
            setOf("https", "android:apk-key-hash").toInputString(),
        )
    }
}
