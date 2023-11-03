package data.storage

import at.asitplus.KmmResult
import at.asitplus.wallet.lib.agent.CredentialToBeIssued
import at.asitplus.wallet.lib.agent.IssuerCredentialDataProvider
import at.asitplus.wallet.lib.cbor.CoseKey
import data.idaustria.IdAustriaCredential
import io.matthewnelson.encoding.base16.Base16
import io.matthewnelson.encoding.core.Encoder.Companion.encodeToString
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

    override fun getCredentialWithType(
        subjectId: String,
        subjectPublicKey: CoseKey?,
        attributeTypes: Collection<String>
    ): KmmResult<List<CredentialToBeIssued>> {
        val expiration = clock.now() + defaultLifetime
        val listOfAttributes = mutableListOf<CredentialToBeIssued>()

        if (attributeTypes.contains(data.idaustria.ConstantIndex.IdAustriaCredential.vcType)) {
            val listFirstname = listOf("Max", "Susanne", "Peter", "Petra", "Hans", "Anna", "Martin", "Barbara")
            val listLastname = listOf("Müller", "Schmidt", "Schneider", "Fischer", "Weber", "Meyer", "Becker", "Koch")
            listOfAttributes.add(
                CredentialToBeIssued.Vc(
                    IdAustriaCredential(
                        id = subjectId,
                        firstname = listFirstname[Random.nextInt(listFirstname.size)],
                        lastname = listLastname[Random.nextInt(listLastname.size)],
                        dateOfBirth = LocalDate(Random.nextInt(from = 1900, until = 2000),1,1),
                        portrait = null
                    ),
                    expiration,
                    data.idaustria.ConstantIndex.IdAustriaCredential.vcType,
                )
            )
        }
        return KmmResult.success(listOfAttributes)
    }
}