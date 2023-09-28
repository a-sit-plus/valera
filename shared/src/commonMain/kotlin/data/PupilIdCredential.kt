package at.asitplus.wallet.pupilid

import at.asitplus.wallet.lib.data.CredentialSubject
import at.asitplus.wallet.lib.data.VerifiableCredential
import at.asitplus.wallet.lib.jws.ByteArrayBase64Serializer
import data.IdHolderCredential
import io.matthewnelson.component.base64.encodeBase64
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * VC spec leaves the representation of a single credential open to implementations.
 * We decided to encode all attributes of a pupil ID into one VC.
 */
@Serializable
@SerialName("PupilId2022")
class PupilIdCredential : CredentialSubject {

    /**
     * "Vorname"
     */
    @SerialName("firstName")
    val firstName: String

    /**
     * "Familienname"
     */
    @SerialName("lastName")
    val lastName: String

    /**
     * "Geburtsdatum"
     */
    @SerialName("dateOfBirth")
    val dateOfBirth: String

    /**
     * "Schulname"
     */
    @SerialName("schoolName")
    val schoolName: String

    /**
     * "Schuladresse Stadt"
     */
    @SerialName("schoolCity")
    val schoolCity: String

    /**
     * "Schuladresse Postleitzahl"
     */
    @SerialName("schoolZip")
    val schoolZip: String

    /**
     * "Schuladresse Straße"
     */
    @SerialName("schoolStreet")
    val schoolStreet: String

    /**
     * "Schulkennzahl"
     */
    @SerialName("schoolId")
    val schoolId: String

    /**
     * "Wohnort"
     */
    @SerialName("pupilCity")
    val pupilCity: String?

    /**
     * "Wohnort-Postleitzahl"
     */
    @SerialName("pupilZip")
    val pupilZip: String?

    /**
     * "Kartennummer"
     */
    @SerialName("cardId")
    val cardId: String

    /**
     * "Gültig bis"
     */
    @SerialName("validUntil")
    val validUntil: String

    /**
     * Hash of "Foto"
     */
    @SerialName("pictureHash")
    @Serializable(with = ByteArrayBase64Serializer::class)
    val pictureHash: ByteArray

    /**
     * Hash of scaled-down "Foto"
     */
    @SerialName("scaledPictureHash")
    @Serializable(with = ByteArrayBase64Serializer::class)
    val scaledPictureHash: ByteArray

    constructor(
        id: String,
        firstName: String,
        lastName: String,
        dateOfBirth: String,
        schoolName: String,
        schoolCity: String,
        schoolZip: String,
        schoolStreet: String,
        schoolId: String,
        pupilCity: String?,
        pupilZip: String?,
        cardId: String,
        validUntil: String,
        pictureHash: ByteArray,
        scaledPictureHash: ByteArray,
    ) : super(id = id) {
        this.firstName = firstName
        this.lastName = lastName
        this.dateOfBirth = dateOfBirth
        this.schoolName = schoolName
        this.schoolCity = schoolCity
        this.schoolZip = schoolZip
        this.schoolStreet = schoolStreet
        this.schoolId = schoolId
        this.pupilCity = pupilCity
        this.pupilZip = pupilZip
        this.cardId = cardId
        this.validUntil = validUntil
        this.pictureHash = pictureHash
        this.scaledPictureHash = scaledPictureHash
    }

    override fun toString(): String {
        return "PupilIdCredential(firstName='$firstName'," +
                " lastName='$lastName'," +
                " dateOfBirth='$dateOfBirth'," +
                " schoolName='$schoolName'," +
                " schoolCity='$schoolCity'," +
                " schoolZip='$schoolZip'," +
                " schoolStreet='$schoolStreet'," +
                " schoolId='$schoolId'," +
                " pupilCity='$pupilCity'," +
                " pupilZip='$pupilZip'," +
                " cardId='$cardId'," +
                " validUntil='$validUntil'," +
                " pictureHash='${pictureHash.encodeBase64()}'" +
                " scaledPictureHash='${scaledPictureHash.encodeBase64()}'" +
                ")"
    }

}

class PupilIdCredentialHolder(val credential: PupilIdCredential, val vc: VerifiableCredential) :
    IdHolderCredential {
    override val displayType: String
        get() = "PupilId"
    override val displayName: String
        get() = credential.firstName + " " + credential.lastName
    override val id: String
        get() = credential.id
    override val vcId: String
        get() = vc.id
    override val attributeTypes: Array<String>
        get() = vc.type
}
