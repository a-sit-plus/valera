package data.storage

import at.asitplus.KmmResult
import at.asitplus.wallet.lib.agent.CredentialToBeIssued
import at.asitplus.wallet.lib.agent.Issuer
import at.asitplus.wallet.lib.agent.IssuerCredentialDataProvider
import at.asitplus.wallet.lib.cbor.CoseKey
import at.asitplus.wallet.lib.data.AtomicAttribute2023
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.iso.DrivingPrivilege
import at.asitplus.wallet.lib.iso.ElementValue
import at.asitplus.wallet.lib.iso.IsoDataModelConstants
import at.asitplus.wallet.lib.iso.IssuerSignedItem
import data.idaustria.IdAustriaCredential
import io.matthewnelson.encoding.base16.Base16
import io.matthewnelson.encoding.core.Encoder.Companion.encodeToString
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

class DummyCredentialDataProvider(
    private val clock: Clock = Clock.System
) : IssuerCredentialDataProvider {

    private val defaultLifetime = 1.minutes

    override fun getCredentialWithType(
        subjectId: String,
        subjectPublicKey: CoseKey?,
        attributeTypes: Collection<String>
    ): KmmResult<List<CredentialToBeIssued>> {
        val attributeType = ConstantIndex.AtomicAttribute2023.vcType
        val expiration = clock.now() + defaultLifetime
        val listOfAttributes = mutableListOf<CredentialToBeIssued>()
        if (attributeTypes.contains(attributeType)) {
            listOfAttributes.addAll(
                listOf(
                    CredentialToBeIssued.Vc(
                        AtomicAttribute2023(subjectId, "given-name", "Susanne"),
                        expiration,
                        attributeType,
                    ),
                    CredentialToBeIssued.Vc(
                        AtomicAttribute2023(subjectId, "family-name", "Meier"),
                        expiration,
                        attributeType,
                    ),
                    CredentialToBeIssued.Vc(
                        AtomicAttribute2023(subjectId, "date-of-birth", "1990-01-01"),
                        expiration,
                        attributeType,
                    ),
                    CredentialToBeIssued.Vc(
                        AtomicAttribute2023(subjectId, "identifier", randomValue()),
                        expiration,
                        attributeType,
                    ),
                    CredentialToBeIssued.Vc(
                        AtomicAttribute2023(subjectId, "picture", randomValue()),
                        expiration,
                        attributeType,
                        listOf(Issuer.Attachment("picture", "image/webp", byteArrayOf(32)))
                    )
                )
            )
        }

        if (attributeTypes.contains(ConstantIndex.MobileDrivingLicence2023.vcType) && subjectPublicKey != null) {
            val drivingPrivilege = DrivingPrivilege(
                vehicleCategoryCode = "B",
                issueDate = LocalDate.parse("2023-01-01"),
                expiryDate = LocalDate.parse("2033-01-31"),
                //codes = arrayOf(DrivingPrivilegeCode(code = "B"))
            )
            val issuerSignedItems = listOf(
                buildIssuerSignedItem(IsoDataModelConstants.DataElements.FAMILY_NAME, "Mustermann", 0U),
                buildIssuerSignedItem(IsoDataModelConstants.DataElements.GIVEN_NAME, "Max", 1U),
                buildIssuerSignedItem(IsoDataModelConstants.DataElements.DOCUMENT_NUMBER, "123456789", 2U),
                buildIssuerSignedItem(IsoDataModelConstants.DataElements.ISSUE_DATE, "2023-01-01", 3U),
                buildIssuerSignedItem(IsoDataModelConstants.DataElements.EXPIRY_DATE, "2033-01-31", 4U),
                //buildIssuerSignedItem(DataElements.DRIVING_PRIVILEGES, drivingPrivilege, 5U),
            )

            listOfAttributes.add(
                CredentialToBeIssued.Iso(
                    issuerSignedItems = issuerSignedItems,
                    subjectPublicKey = subjectPublicKey,
                    expiration = expiration,
                    attributeType = at.asitplus.wallet.lib.data.ConstantIndex.MobileDrivingLicence2023.vcType,
                )
            )
        }

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

    private fun randomValue() = Random.nextBytes(32).encodeToString(Base16(strict = true))

    fun buildIssuerSignedItem(elementIdentifier: String, elementValue: String, digestId: UInt) = IssuerSignedItem(
        digestId = digestId,
        random = Random.nextBytes(16),
        elementIdentifier = elementIdentifier,
        elementValue = ElementValue(string = elementValue)
    )

    fun buildIssuerSignedItem(elementIdentifier: String, elementValue: DrivingPrivilege, digestId: UInt) =
        IssuerSignedItem(
            digestId = digestId,
            random = Random.nextBytes(16),
            elementIdentifier = elementIdentifier,
            elementValue = ElementValue(drivingPrivilege = arrayOf(elementValue))
        )
}