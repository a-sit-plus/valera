package data.credentials

import at.asitplus.iso.IssuerSignedItem
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.openid.OidcUserInfo
import at.asitplus.openid.OidcUserInfoExtended
import at.asitplus.wallet.lib.agent.CredentialToBeIssued
import at.asitplus.wallet.lib.agent.EphemeralKeyWithSelfSignedCert
import at.asitplus.wallet.lib.agent.EphemeralKeyWithoutCert
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.IssuerAgent
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.agent.Validator
import at.asitplus.wallet.lib.agent.toStoreCredentialInput
import at.asitplus.wallet.lib.data.IsoMdocFallbackCredentialScheme
import at.asitplus.wallet.lib.data.rfc3986.toUri
import data.Attribute
import data.storage.DummyDataStoreService
import data.storage.PersistentSubjectCredentialStore
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

/**
 * A consent list must never under-report what is about to be disclosed. The requested paths are built by the
 * matching layer rather than taken from the credential, and [NormalizedJsonPath] has no value equality, so an
 * attribute lookup keyed on path identity silently drops every row it cannot resolve.
 */
class DisclosedAttributeLabelingTest {

    @Test
    fun labelsEveryDisclosedAttributeOfCredentialWithUnknownScheme() = runTest {
        val scheme = IsoMdocFallbackCredentialScheme(DOCTYPE)
        val credential = storeIsoCredential(scheme)
        // rebuild the paths the way the matching layer does: equal in value to the credential's own, but not identical
        val requested = credential.toGenericAttributeList()
            .map { (path, _) -> path.segments.fold(NormalizedJsonPath()) { rebuilt, segment -> rebuilt + segment } }
        assertEquals(2, requested.size, "test credential should carry both issued data elements")

        val labeled = credential.labeledDisclosedAttributes(scheme, requested) { Result.failure(NotImplementedError()) }

        assertEquals(requested.size, labeled.size, "no requested attribute may be dropped from a consent list")
        assertTrue(labeled.all { it.first.isNotBlank() }, "every disclosed attribute needs a label, got: $labeled")
        assertEquals(
            listOf(Attribute.StringAttribute("Musterfrau"), Attribute.StringAttribute("Erika")),
            labeled.map { it.second },
            "every disclosed attribute needs its value, in request order",
        )
    }

    private suspend fun storeIsoCredential(
        scheme: IsoMdocFallbackCredentialScheme,
    ): SubjectCredentialStore.StoreEntry {
        val keyMaterial = EphemeralKeyWithoutCert()
        val store = PersistentSubjectCredentialStore(DummyDataStoreService(), Validator())
        HolderAgent(keyMaterial = keyMaterial, subjectCredentialStore = store).storeCredential(
            IssuerAgent(
                keyMaterial = EphemeralKeyWithSelfSignedCert(),
                identifier = "https://issuer.example.com/".toUri(),
            ).issueCredential(
                CredentialToBeIssued.Iso(
                    issuerSignedItems = listOf(
                        IssuerSignedItem(0U, Random.nextBytes(16), "family_name", "Musterfrau"),
                        IssuerSignedItem(1U, Random.nextBytes(16), "given_name", "Erika"),
                    ),
                    expiration = Clock.System.now() + 10.minutes,
                    scheme = scheme,
                    subjectPublicKey = keyMaterial.publicKey,
                    userInfo = OidcUserInfoExtended.fromOidcUserInfo(OidcUserInfo("subject")).getOrThrow(),
                )
            ).getOrThrow().toStoreCredentialInput()
        ).getOrThrow()
        return store.getCredentials().getOrThrow().single()
    }

    companion object {
        private const val DOCTYPE = "org.example.unknown.1"
    }
}
