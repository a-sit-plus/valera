package data.storage

import at.asitplus.KmmResult
import at.asitplus.wallet.lib.CryptoPublicKey
import at.asitplus.wallet.lib.agent.CredentialToBeIssued
import at.asitplus.wallet.lib.agent.IssuerCredentialDataProvider
import at.asitplus.wallet.lib.data.ConstantIndex
import data.idaustria.IdAustriaCredential
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
/**
 * Temporary class to create random credentials
 */
class DummyCredentialDataProvider(
    private val clock: Clock = Clock.System
) : IssuerCredentialDataProvider {

    private val defaultLifetime = 1.minutes

    override fun getCredential(
        subjectPublicKey: CryptoPublicKey,
        credentialScheme: ConstantIndex.CredentialScheme,
        representation: ConstantIndex.CredentialRepresentation
    ): KmmResult<List<CredentialToBeIssued>> {
        val expiration = clock.now() + defaultLifetime
        val listOfAttributes = mutableListOf<CredentialToBeIssued>()

        if (credentialScheme == data.idaustria.ConstantIndex.IdAustriaCredential) {
            val listFirstname = listOf("Max", "Susanne", "Peter", "Petra", "Hans", "Anna", "Martin", "Barbara")
            val listLastname = listOf("Müller", "Schmidt", "Schneider", "Fischer", "Weber", "Meyer", "Becker", "Koch")
            listOfAttributes.add(
                CredentialToBeIssued.VcJwt(
                    IdAustriaCredential(
                        id = subjectPublicKey.toJsonWebKey().identifier,
                        firstname = listFirstname[Random.nextInt(listFirstname.size)],
                        lastname = listLastname[Random.nextInt(listLastname.size)],
                        dateOfBirth = LocalDate(Random.nextInt(from = 1900, until = 2000),1,1),
                        portrait = null
                    ),
                    expiration,
                    null,
                )
            )
        }
        return KmmResult.success(listOfAttributes)
    }
}