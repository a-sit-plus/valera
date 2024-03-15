import Foundation
import CryptoKit
import shared

public class VcLibCryptoServiceCryptoKit: CryptoService {
    public var publicKey: DatatypesCryptoPublicKey
    public var identifier: String
    public var coseKey: CoseKey
    public var algorithm: DatatypesCryptoAlgorithm
    public var jsonWebKey: JsonWebKey
    public var certificate: DatatypesX509Certificate
    private let keyChainService: KeyChainService

    public init?(keyChainService: KeyChainService) {
        NapierProxy.companion.d(msg: "Init VcLibCryptoServiceCryptoKit")
        guard let privateKey = keyChainService.loadPrivateKey() else {
            return nil
        }
        self.keyChainService = keyChainService
        guard let publicKey = try? DatatypesCryptoPublicKey.companion.fromIosEncoded(it: privateKey.publicKey.x963Representation.kotlinByteArray) else {
            NapierProxy.companion.w(msg: "Cannot convert publicKey")
            return nil
        }
        self.publicKey = publicKey
        self.identifier = publicKey.toJsonWebKey().identifier
        self.algorithm = .es256
        // TODO self-signed certificate?! or remove in vclib
        self.certificate = DatatypesX509Certificate.companion.decodeFromTlv(src: DatatypesAsn1Sequence.companion.decodeFromDerHexString(derEncoded: "TODO") as! DatatypesAsn1Sequence)
    }

    public func decrypt(key: KotlinByteArray, iv: KotlinByteArray, aad: KotlinByteArray, input: KotlinByteArray, authTag: KotlinByteArray, algorithm: JweEncryption) async throws -> KmmResult<KotlinByteArray> {
        switch algorithm {
        case .a256gcm:
            let key = SymmetricKey(data: key.data)
            guard let nonce = try? AES.GCM.Nonce(data: iv.data),
                  let sealedBox = try? AES.GCM.SealedBox(nonce: nonce, ciphertext: input.data, tag: authTag.data),
                  let decryptedData = try? AES.GCM.open(sealedBox, using: key, authenticating: aad.data) else {
                return KmmResultFailure(KotlinThrowable(message: "Error in AES.GCM.open"))
            }
            return KmmResultSuccess(decryptedData.kotlinByteArray)
        default:
            return KmmResultFailure(KotlinThrowable(message: "Algorithm unknown \(algorithm)"))
        }
    }

    public func encrypt(key: KotlinByteArray, iv: KotlinByteArray, aad: KotlinByteArray, input: KotlinByteArray, algorithm: JweEncryption) -> KmmResult<AuthenticatedCiphertext> {
        switch algorithm {
        case .a256gcm:
            let key = SymmetricKey(data: key.data)
            guard let nonce = try? AES.GCM.Nonce(data: iv.data),
                  let encryptedData = try? AES.GCM.seal(input.data, using: key, nonce: nonce, authenticating: aad.data) else {
                return KmmResultFailure(KotlinThrowable(message: "Error in AES.GCM.seal"))
            }
            let ac = AuthenticatedCiphertext(ciphertext: encryptedData.ciphertext.kotlinByteArray, authtag: encryptedData.tag.kotlinByteArray)
            return KmmResultSuccess(ac)
        default:
            return KmmResultFailure(KotlinThrowable(message: "Algorithm unknown \(algorithm)"))
        }
    }

    public func generateEphemeralKeyPair(ecCurve: DatatypesEcCurve) -> KmmResult<EphemeralKeyHolder> {
        switch ecCurve {
        case .secp256R1:
            return KmmResultSuccess(VcLibEphemeralKeyHolder())
        default:
            return KmmResultFailure(KotlinThrowable(message: "ecCurve unknown \(ecCurve)"))
        }
    }

    public func messageDigest(input: KotlinByteArray, digest: DatatypesDigest) -> KmmResult<KotlinByteArray> {
        switch digest {
        case .sha256:
            let digest = SHA256.hash(data: input.data)
            let data = Data(digest.compactMap { $0 })
            return KmmResultSuccess(data.kotlinByteArray)
        default:
            return KmmResultFailure(KotlinThrowable(message: "Digest unknown \(digest)"))
        }
    }

    public func performKeyAgreement(ephemeralKey: EphemeralKeyHolder, recipientKey: JsonWebKey, algorithm: JweAlgorithm) -> KmmResult<KotlinByteArray> {
        switch algorithm {
        case .ecdhEs:
            let recipientKeyBytes = recipientKey.toCryptoPublicKey()
            if let throwable = recipientKeyBytes.exceptionOrNull() {
                return KmmResultFailure(throwable)
            }
            guard let recipientKeyBytesValue = recipientKeyBytes.getOrNull(),
                  let recipientKey = try? P256.KeyAgreement.PublicKey(x963Representation: recipientKeyBytesValue.iosEncoded.data),
                  let ephemeralKey = ephemeralKey as? VcLibEphemeralKeyHolder,
                  let sharedSecret = try? ephemeralKey.privateKey.sharedSecretFromKeyAgreement(with: recipientKey) else {
                return KmmResultFailure(KotlinThrowable(message: "Error in KeyAgreement"))
            }
            let data = sharedSecret.withUnsafeBytes {
                return Data($0)
            }
            return KmmResultSuccess(data.kotlinByteArray)
        default:
            return KmmResultFailure(KotlinThrowable(message: "Algorithm unknown \(algorithm)"))
        }
    }

    public func performKeyAgreement(ephemeralKey: JsonWebKey, algorithm: JweAlgorithm) -> KmmResult<KotlinByteArray> {
        switch algorithm {
        case .ecdhEs:
            guard let privateKey = keyChainService.loadPrivateKey() else {
                return KmmResultFailure(KotlinThrowable(message: "Could not load private key"))
            }
            let cryptoPublicKey = ephemeralKey.toCryptoPublicKey()
            if let throwable = cryptoPublicKey.exceptionOrNull() {
                return KmmResultFailure(throwable)
            }
            guard let cryptoPublicKeyValue = cryptoPublicKey.getOrNull(),
                  let recipientKey = try? P256.KeyAgreement.PublicKey(x963Representation: cryptoPublicKeyValue.iosEncoded.data),
                  let privateAgreementKey = try? SecureEnclave.P256.KeyAgreement.PrivateKey(dataRepresentation: privateKey.dataRepresentation),
                  let sharedSecret = try? privateAgreementKey.sharedSecretFromKeyAgreement(with: recipientKey) else {
                return KmmResultFailure(KotlinThrowable(message: "Error in KeyAgreement"))
            }
            let data = sharedSecret.withUnsafeBytes {
                return Data($0)
            }
            return KmmResultSuccess(data.kotlinByteArray)
        default:
            return KmmResultFailure(KotlinThrowable(message: "Algorithm unknown \(algorithm)"))
        }
    }

    public func sign(input: KotlinByteArray) async throws -> KmmResult<DatatypesCryptoSignature> {
        guard let privateKey = keyChainService.loadPrivateKey() else {
            return KmmResultFailure(KotlinThrowable(message: "Could not load private key"))
        }
        guard let signature = try? privateKey.signature(for: input.data) else {
            return KmmResultFailure(KotlinThrowable(message: "Signature error"))
        }
        // TODO why is this cast needed?
        return KmmResultSuccess(DatatypesCryptoSignature.companion.decodeFromDer(src: signature.derRepresentation.kotlinByteArray) as! DatatypesCryptoSignature)
    }


}

public class VcLibVerifierCryptoService : VerifierCryptoService {
    
    public var supportedAlgorithms: [DatatypesCryptoAlgorithm] = [.es256]

    public func verify(input: KotlinByteArray, signature: DatatypesCryptoSignature, algorithm: DatatypesCryptoAlgorithm, publicKey: DatatypesCryptoPublicKey) -> KmmResult<KotlinBoolean> {
        if algorithm != .es256 {
            return KmmResultFailure(KotlinThrowable(message: "Can not verify algorithm \(algorithm.name)"))
        }
        guard let cryptoKitPublicKey = try? P256.Signing.PublicKey(x963Representation: publicKey.iosEncoded.data) else {
            return KmmResultFailure(KotlinThrowable(message: "Can not create CryptoKit key"))
        }
        if let cryptoKitSignature = try? P256.Signing.ECDSASignature(derRepresentation: signature.rawByteArray.data) {
            let valid = cryptoKitPublicKey.isValidSignature(cryptoKitSignature, for: input.data)
            return KmmResultSuccess(KotlinBoolean(value: valid))
        } else if let cryptoKitSignature = try? P256.Signing.ECDSASignature(rawRepresentation: signature.rawByteArray.data) {
            let valid = cryptoKitPublicKey.isValidSignature(cryptoKitSignature, for: input.data)
            return KmmResultSuccess(KotlinBoolean(value: valid))
        } else {
            return KmmResultFailure(KotlinThrowable(message: "Can not read signature"))
        }
    }

}

public class VcLibEphemeralKeyHolder : EphemeralKeyHolder {

    let privateKey: P256.KeyAgreement.PrivateKey
    let publicKey: P256.KeyAgreement.PublicKey
    public var publicJsonWebKey: JsonWebKey?

    public init() {
        self.privateKey = P256.KeyAgreement.PrivateKey()
        self.publicKey = privateKey.publicKey
        self.publicJsonWebKey = JsonWebKey.companion.fromIosEncoded(bytes: publicKey.x963Representation.kotlinByteArray).getOrNull()
    }

}
