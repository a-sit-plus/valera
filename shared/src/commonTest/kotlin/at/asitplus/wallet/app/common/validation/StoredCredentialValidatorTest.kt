package at.asitplus.wallet.app.common.validation

import at.asitplus.iso.IssuerSignedItem
import at.asitplus.openid.OidcUserInfo
import at.asitplus.openid.OidcUserInfoExtended
import at.asitplus.wallet.lib.agent.CredentialToBeIssued
import at.asitplus.wallet.lib.agent.EphemeralKeyWithSelfSignedCert
import at.asitplus.wallet.lib.agent.EphemeralKeyWithoutCert
import at.asitplus.wallet.lib.agent.Issuer
import at.asitplus.wallet.lib.agent.IssuerAgent
import at.asitplus.wallet.lib.agent.SubjectCredentialStore.StoreEntry
import at.asitplus.wallet.lib.data.IsoMdocFallbackCredentialScheme
import at.asitplus.wallet.lib.data.rfc3986.toUri
import at.asitplus.wallet.lib.validation.CredentialValidator
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class StoredCredentialValidatorTest {

    @Test
    fun reusesTheFreshnessOfACredentialCheckedRecently() = runTest {
        val validator = StoredCredentialValidator(CredentialValidator())
        val entry = storeEntry()

        assertSame(validator.freshness(entry), validator.freshness(entry))
    }

    @Test
    fun checksAnotherCredentialAgain() = runTest {
        val validator = StoredCredentialValidator(CredentialValidator())

        assertNotSame(validator.freshness(storeEntry()), validator.freshness(storeEntry()))
    }

    private suspend fun storeEntry(): StoreEntry.Iso {
        val issued = IssuerAgent(
            keyMaterial = EphemeralKeyWithSelfSignedCert(),
            identifier = "https://issuer.example.com/".toUri(),
        ).issueCredential(
            CredentialToBeIssued.Iso(
                issuerSignedItems = listOf(IssuerSignedItem(0U, Random.nextBytes(16), "family_name", "Musterfrau")),
                expiration = Clock.System.now() + 10.minutes,
                scheme = IsoMdocFallbackCredentialScheme(DOCTYPE),
                subjectPublicKey = EphemeralKeyWithoutCert().publicKey,
                userInfo = OidcUserInfoExtended.fromOidcUserInfo(OidcUserInfo("subject")).getOrThrow(),
            )
        ).getOrThrow() as Issuer.IssuedCredential.Iso
        return StoreEntry.Iso(issued.issuerSigned, schemeIdentifier = DOCTYPE)
    }

    private companion object {
        const val DOCTYPE = "org.example.unknown.1"
    }
}
