package data.storage

import DataStoreService
import at.asitplus.KmmResult
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.agent.CredentialToBeIssued
import at.asitplus.wallet.lib.agent.DefaultCryptoService
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.Issuer
import at.asitplus.wallet.lib.agent.IssuerAgent
import at.asitplus.wallet.lib.agent.IssuerCredentialDataProvider
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.cbor.CoseKey
import at.asitplus.wallet.lib.data.AtomicAttribute2023
import at.asitplus.wallet.lib.data.CredentialSubject
import at.asitplus.wallet.lib.data.VerifiableCredential
import at.asitplus.wallet.lib.data.VerifiableCredentialJws
import at.asitplus.wallet.lib.data.jsonSerializer
import at.asitplus.wallet.lib.iso.DrivingPrivilege
import at.asitplus.wallet.lib.iso.ElementValue
import at.asitplus.wallet.lib.iso.IsoDataModelConstants.DataElements
import at.asitplus.wallet.lib.iso.IssuerSigned
import at.asitplus.wallet.lib.iso.IssuerSignedItem
import data.idaustria.IdAustriaCredential
import io.github.aakira.napier.Napier
import io.matthewnelson.encoding.base16.Base16
import io.matthewnelson.encoding.core.Encoder.Companion.encodeToString
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.serialization.encodeToString
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes







suspend fun getCredentials(storageService: SubjectCredentialStore): ArrayList<CredentialSubject> {
    val credentialList = ArrayList<CredentialSubject>()
    val storeEntries = storageService.getCredentials(null).getOrThrow()
    storeEntries.forEach {entry ->
        when(entry) {
            is SubjectCredentialStore.StoreEntry.Iso -> TODO()
            is SubjectCredentialStore.StoreEntry.Vc -> when (val subject = entry.vc.vc.credentialSubject) {
                is AtomicAttribute2023 -> {
                    credentialList.add(subject)
                }
                is IdAustriaCredential -> {
                    credentialList.add(subject)
                }
            }

            else -> {}
        }
    }
    return credentialList
}

suspend fun removeCredentialById(storageService: SubjectCredentialStore, id: String) {
    when (storageService) {
        is PersistentSubjectCredentialStore -> {
            storageService.removeCredential(id)
        }
    }
}


class PersistentSubjectCredentialStore(private val dataStore: DataStoreService) : SubjectCredentialStore {
    private val dataKey = "VCs"
    private val idHolder: IdHolder = runBlocking { importFromDataStore() }

    override suspend fun getAttachment(name: String): KmmResult<ByteArray> {
        return KmmResult(ByteArray(0))
    }

    override suspend fun getAttachment(name: String, vcId: String): KmmResult<ByteArray> {
        return KmmResult(ByteArray(0))
    }

    override suspend fun getCredentials(requiredAttributeTypes: Collection<String>?): KmmResult<List<SubjectCredentialStore.StoreEntry>> {
        val filtered = idHolder.credentials
            .filter { it ->
                val vc = it.vc
                requiredAttributeTypes?.let { types ->
                    vc.type.any { it in types }
                } ?: true
            }

        val approved = getCredentialsInternal(requiredAttributeTypes)
        return KmmResult.success(
            filtered.filter { it ->
                val vc = it.vc
                vc.type.any { approved.contains(it) }
            }
                .map {
                    it.let { vc ->
                        SubjectCredentialStore.StoreEntry.Vc(vc.serialize(), vc)
                    }
                }
                .toList())
    }

    private fun getCredentialsInternal(
        requiredAttributeTypes: Collection<String>?
    ): Collection<String> {
            val content = requiredAttributeTypes
                ?: idHolder.credentials.map {
                    val vc = it.vc
                    (vc.type).toList()
                }.flatten()
                    .filter { it != "NULL" }
                    .filter { it != "VerifiableCredential" }
                    .distinct().toList()
        return content
    }

    override suspend fun storeAttachment(name: String, data: ByteArray, vcId: String) {
        }

    override suspend fun storeCredential(vc: VerifiableCredentialJws, vcSerialized: String) {
        Napier.d("storing $vcSerialized")
        // TODO CK analyze usage of attrName
        val attrName = (vc.vc.credentialSubject as? AtomicAttribute2023)?.name
            ?: "NULL"
        val attrTypes = vc.vc.type
        idHolder.credentials.add(vc)
        exportToDataStore()
    }

    override suspend fun storeCredential(issuerSigned: IssuerSigned) {
        println("TODO")
        TODO("Not yet implemented")
    }

    private suspend fun exportToDataStore() {
        runBlocking {
            val json = jsonSerializer.encodeToString(idHolder)
            dataStore.setData(value = json, key = dataKey)
        }
    }

    private suspend fun importFromDataStore(): IdHolder {
        val input = dataStore.getData(dataKey)
        return jsonSerializer.decodeFromString(input.toString()) ?: IdHolder()
    }
    suspend fun removeCredential(id: String) {
        var found: VerifiableCredentialJws? = null
        idHolder.credentials.forEach {
            val vc = it.vc
            if (vc.id == id) {
                found = it
            }
        }
        if (found != null) {
            idHolder.credentials.remove(found)
            exportToDataStore()
        }
    }
}

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
                buildIssuerSignedItem(DataElements.FAMILY_NAME, "Mustermann", 0U),
                buildIssuerSignedItem(DataElements.GIVEN_NAME, "Max", 1U),
                buildIssuerSignedItem(DataElements.DOCUMENT_NUMBER, "123456789", 2U),
                buildIssuerSignedItem(DataElements.ISSUE_DATE, "2023-01-01", 3U),
                buildIssuerSignedItem(DataElements.EXPIRY_DATE, "2033-01-31", 4U),
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