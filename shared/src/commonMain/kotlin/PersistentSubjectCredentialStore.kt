import at.asitplus.KmmResult
import at.asitplus.wallet.lib.agent.CredentialToBeIssued
import at.asitplus.wallet.lib.agent.DefaultCryptoService
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.Issuer
import at.asitplus.wallet.lib.agent.IssuerAgent
import at.asitplus.wallet.lib.agent.IssuerCredentialDataProvider
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.cbor.CoseKey
import at.asitplus.wallet.lib.data.AtomicAttribute2023
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.VerifiableCredentialJws
import at.asitplus.wallet.lib.iso.DrivingPrivilege
import at.asitplus.wallet.lib.iso.ElementValue
import at.asitplus.wallet.lib.iso.IsoDataModelConstants.DataElements
import at.asitplus.wallet.lib.iso.IssuerSigned
import at.asitplus.wallet.lib.iso.IssuerSignedItem
import data.IdHolder
import data.IdList
import data.IdVc
import io.github.aakira.napier.Napier
import io.matthewnelson.encoding.base16.Base16
import io.matthewnelson.encoding.core.Encoder.Companion.encodeToString
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes


suspend fun testDataStore(){
    globalData.delData("VCs")
    setCredentials(PersistentSubjectCredentialStore())
    getCredentials(PersistentSubjectCredentialStore())


}

suspend fun setCredentials(storageService: SubjectCredentialStore){
    val holderAgent = HolderAgent.newDefaultInstance(cryptoService = globalCrypto, subjectCredentialStore =  storageService)
    runBlocking {
        holderAgent.storeCredentials(
            IssuerAgent.newDefaultInstance(
                DefaultCryptoService(),
                dataProvider = DummyCredentialDataProvider(), // TODO neu implementieren, mit IDAustriaCredential
            ).issueCredentialWithTypes(
                holderAgent.identifier,
                attributeTypes = listOf(ConstantIndex.AtomicAttribute2023.vcType) // hier auch IDAustriaCredential
            ).toStoreCredentialInput()
        )
    }
}

suspend fun getCredentials(storageService: SubjectCredentialStore){
    val storeEntries = storageService.getCredentials(null).getOrThrow()
    storeEntries.forEach {entry ->
        when(entry) {
            is SubjectCredentialStore.StoreEntry.Iso -> TODO()
            is SubjectCredentialStore.StoreEntry.Vc -> when (val subject = entry.vc.vc.credentialSubject) {
                is AtomicAttribute2023 -> {
                    println(subject.name)
                    println(subject.value)
                }
            }

            else -> {}
        }
    }
}


class PersistentSubjectCredentialStore() : SubjectCredentialStore {
    private val dataStore = globalData
    private val KEY = "VCs"

    override suspend fun getAttachment(name: String): KmmResult<ByteArray> {
        val attachment = getIdList().idHolders.firstNotNullOfOrNull { it.attachments[name] }
        return attachment?.let { KmmResult.success(it) }
            ?: KmmResult.failure(Exception("Attachment $name not found"))
    }

    override suspend fun getAttachment(name: String, vcId: String): KmmResult<ByteArray> {
        return getIdList().idHolders.firstOrNull { it.id == vcId }?.attachments?.get(name)
            ?.let { KmmResult.success(it) }
            ?: KmmResult.failure(Exception("Attachment $name not found"))
    }

    override suspend fun getCredentials(requiredAttributeTypes: Collection<String>?): KmmResult<List<SubjectCredentialStore.StoreEntry>> {
        val filtered = getIdList().idHolders
            .asSequence()
            .map { it.credentials }.flatten()
            .filter {
                requiredAttributeTypes?.let { types ->
                    it.attrTypes.any { it in types }
                } ?: true
            }
        val approved = getCredentialsInternal(getIdList().idHolders, requiredAttributeTypes)
        return KmmResult.success(
            filtered.filter {
                approved.contains(it.attrName) || it.attrTypes.any { approved.contains(it) }
            }
                .mapNotNull {
                    VerifiableCredentialJws.deserialize(it.vcJwsSerialized)?.let { vc ->
                        SubjectCredentialStore.StoreEntry.Vc(it.vcSerialized, vc)

                    }
                }
                .toList())
    }

    private suspend fun getCredentialsInternal(
        idList: ArrayList<IdHolder>,
        requiredAttributeTypes: Collection<String>?
    ): Collection<String> {
            val content = requiredAttributeTypes
                ?: idList.asSequence().map { it.credentials }.flatten().map { idVc ->
                    (idVc.attrTypes + idVc.attrName).toList()
                }.flatten()
                    .filter { it != "NULL" }
                    .filter { it != "VerifiableCredential" }
                    .distinct().toList()
        return content
    }

    override suspend fun storeAttachment(name: String, data: ByteArray, vcId: String) {
            Napier.d("storing attachment $name in VC $vcId")
            val idList = getIdListForEdit()
            val matchingHolder = idList.idHolders.getHolderFromVcId(vcId)
            if (matchingHolder != null) {
                matchingHolder.attachments[name] = data
            } else {
                Napier.e("No vc with ID $vcId found to store the attachment $name")
            }
            updateIdListAfterEdit(idList)
        }

    override suspend fun storeCredential(vc: VerifiableCredentialJws, vcSerialized: String) {
        Napier.d("storing $vcSerialized")
        val idList = getIdListForEdit()
        val idHolder = idList.getOrCreate(vc.subject)
        // TODO CK analyze usage of attrName
        val attrName = (vc.vc.credentialSubject as? AtomicAttribute2023)?.name
            ?: "NULL"
        val attrTypes = vc.vc.type
        idHolder.credentials.add(IdVc(attrName, attrTypes, vcSerialized, vc.serialize()))
        updateIdListAfterEdit(idList)
    }

    override suspend fun storeCredential(issuerSigned: IssuerSigned) {
        println("TODO")
        TODO("Not yet implemented")
    }

    private fun getIdListForEdit(): IdList {
        return runBlocking {  dataStore.getData(KEY)?.let { IdList.deserialize(it) } ?: IdList(arrayListOf()) }
    }

    private fun updateIdListAfterEdit(idList: IdList) {
        runBlocking { dataStore.setData(value = idList.serialize(), key = KEY) }

    }

    suspend fun getIdList(): IdList {
        return getIdListForEdit()
    }
}


class DummyCredentialDataProvider(
    private val clock: Clock = Clock.System,
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
                    attributeType = ConstantIndex.MobileDrivingLicence2023.vcType,
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

fun ArrayList<IdHolder>.getHolderFromVcId(vcId: String): IdHolder?{
    for (it in this){
        if (it.vcIdExist(vcId)){
            return it
        }
    }
    return null
}