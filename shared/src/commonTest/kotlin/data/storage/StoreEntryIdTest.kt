package data.storage

import at.asitplus.iso.IssuerSignedItem
import at.asitplus.openid.OidcUserInfo
import at.asitplus.openid.OidcUserInfoExtended
import at.asitplus.wallet.lib.agent.CredentialToBeIssued
import at.asitplus.wallet.lib.agent.EphemeralKeyWithSelfSignedCert
import at.asitplus.wallet.lib.agent.EphemeralKeyWithoutCert
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.IssuerAgent
import at.asitplus.wallet.lib.agent.toStoreCredentialInput
import at.asitplus.wallet.lib.data.IsoMdocFallbackCredentialScheme
import at.asitplus.wallet.lib.data.rfc3986.toUri
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class StoreEntryIdTest {
    private val keyMaterial = EphemeralKeyWithoutCert()
    private val issuer = IssuerAgent(
        keyMaterial = EphemeralKeyWithSelfSignedCert(),
        identifier = "https://issuer.example.com/".toUri(),
    )

    @Test
    fun knowsTheIdsOfStoredEntries() = runTest {
        val store = PersistentSubjectCredentialStore(DummyDataStoreService())
        val holder = HolderAgent(keyMaterial = keyMaterial, subjectCredentialStore = store)

        val stored = listOf("Erika", "Max").map { holder.storeCredential(issue(it)).getOrThrow() }

        assertEquals(
            store.observeStoreContainer().first().credentials.map { it.first },
            stored.map { store.storeEntryIdOf(it) },
        )
    }

    private suspend fun issue(givenName: String) = issuer.issueCredential(
        CredentialToBeIssued.Iso(
            issuerSignedItems = listOf(IssuerSignedItem(0U, Random.nextBytes(16), "given_name", givenName)),
            expiration = Clock.System.now() + 10.minutes,
            scheme = IsoMdocFallbackCredentialScheme("org.example.unknown.1"),
            subjectPublicKey = keyMaterial.publicKey,
            userInfo = OidcUserInfoExtended.fromOidcUserInfo(OidcUserInfo("subject")).getOrThrow(),
        )
    ).getOrThrow().toStoreCredentialInput()
}
