package at.asitplus.wallet.app.common.dcapi.data.export

import at.asitplus.jsonpath.core.NormalizedJsonPath
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SdJwtEntryTest {
    private val labels = mapOf(
        "nationalities" to "Nationalities",
        "family_name" to "Family name",
        "address.country" to "Country",
    )

    private fun label(path: NormalizedJsonPath): String? =
        labels[path.toDcqlPathKey()]

    @Test
    fun exportsPrimitiveAndNestedClaimsUnderJoinedNames() {
        val claims = SdJwtEntry.fromAttributeList(
            listOf(
                (NormalizedJsonPath() + "family_name") to JsonPrimitive("Mustermann"),
                (NormalizedJsonPath() + "address" + "country") to JsonPrimitive("AT"),
            ),
            ::label,
        )

        assertEquals(setOf("family_name", "address.country"), claims.keys)
        assertEquals(ExportedElements("Family name", "Mustermann", "Mustermann"), claims["family_name"])
        assertEquals(ExportedElements("Country", "AT", "AT"), claims["address.country"])
    }

    @Test
    fun exportsArrayOfStringsPerIndexAndAsWildcard() {
        val nationalities = buildJsonArray {
            add(JsonPrimitive("AT"))
            add(JsonPrimitive("DE"))
        }

        val claims = SdJwtEntry.fromAttributeList(
            listOf((NormalizedJsonPath() + "nationalities") to nationalities),
            ::label,
        )

        assertEquals(
            setOf("nationalities", "nationalities.0", "nationalities.1", "nationalities.null"),
            claims.keys,
        )
        assertEquals(ExportedElements("Nationalities", "AT", "AT"), claims["nationalities.0"])
        assertEquals(ExportedElements("Nationalities", "DE", "DE"), claims["nationalities.1"])
        // DCQL path ["nationalities", null], as requested e.g. by the EUDI reference verifier
        assertEquals(ExportedElements("Nationalities", """["AT","DE"]""", ""), claims["nationalities.null"])
    }

    @Test
    fun exportsArrayOfObjectsPerIndexAndAsWildcardProjection() {
        val addresses = buildJsonArray {
            add(buildJsonObject { put("country", "AT"); put("locality", "Graz") })
            add(buildJsonObject { put("country", "DE") })
        }

        val claims = SdJwtEntry.fromAttributeList(
            listOf((NormalizedJsonPath() + "address") to addresses),
            ::label,
        )

        assertEquals(ExportedElements("Country", "AT", "AT"), claims["address.0.country"])
        assertEquals(ExportedElements("locality", "Graz", "Graz"), claims["address.0.locality"])
        assertEquals(ExportedElements("Country", "DE", "DE"), claims["address.1.country"])
        assertNull(claims["address.1.locality"])
        assertEquals(ExportedElements("Country", """["AT","DE"]""", ""), claims["address.null.country"])
        assertEquals(ExportedElements("locality", """["Graz"]""", ""), claims["address.null.locality"])
    }

    @Test
    fun doesNotExportWildcardForEmptyArray() {
        val claims = SdJwtEntry.fromAttributeList(
            listOf((NormalizedJsonPath() + "nationalities") to buildJsonArray { }),
            ::label,
        )

        assertEquals(setOf("nationalities"), claims.keys)
    }
}
