import Foundation
import CryptoKit
import shared

public class VcLibCryptoServiceCryptoKit: CryptoService {
 
    public var identifier: String
    public var jwsAlgorithm: JwsAlgorithm
    public var coseAlgorithm: CoseAlgorithm
    public var certificate: KotlinByteArray?
    private let cryptoPublicKey: CryptoPublicKey
    private let keyChainService: KeyChainService

    public init?(keyChainService: KeyChainService) {
        guard let privateKey = keyChainService.loadPrivateKey() else {
            return nil
        }
        self.keyChainService = keyChainService
        self.cryptoPublicKey = CryptoPublicKey.Ec.companion.fromAnsiX963Bytes(curve: .secp256R1, it: privateKey.publicKey.x963Representation.kotlinByteArray)!
        self.identifier = cryptoPublicKey.toJsonWebKey().identifier
        self.jwsAlgorithm = .es256
        self.coseAlgorithm = .es256
        self.certificate = nil
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

    public func generateEphemeralKeyPair(ecCurve: EcCurve) -> KmmResult<EphemeralKeyHolder> {
        switch ecCurve {
        case .secp256R1:
            return KmmResultSuccess(VcLibEphemeralKeyHolder())
        default:
            return KmmResultFailure(KotlinThrowable(message: "ecCurve unknown \(ecCurve)"))
        }
    }

    public func messageDigest(input: KotlinByteArray, digest: VcLibKMMDigest) -> KmmResult<KotlinByteArray> {
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
            let recipientKeyBytes = recipientKey.toAnsiX963ByteArray()
            if let throwable = recipientKeyBytes.exceptionOrNull() {
                return KmmResultFailure(throwable)
            }
            guard let recipientKeyBytesValue = recipientKeyBytes.getOrNull(),
                  let recipientKey = try? P256.KeyAgreement.PublicKey(x963Representation: recipientKeyBytesValue.data),
                  let ephemeralKey = ephemeralKey as? VcLibEphemeralKeyHolder,
                  let sharedSecret = try? ephemeralKey.privateKey.sharedSecretFromKeyAgreement(with: recipientKey) else {
                return KmmResultFailure(KotlinThrowable(message: "Error in KeyAgreement"))
            }
            let data = sharedSecret.withUnsafeBytes {
                return Data(Array($0))
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
            let ephemeralKeyBytes = ephemeralKey.toAnsiX963ByteArray()
            if let throwable = ephemeralKeyBytes.exceptionOrNull() {
                return KmmResultFailure(throwable)
            }
            guard let recipientKeyBytesValue = ephemeralKeyBytes.getOrNull(),
                  let recipientKey = try? P256.KeyAgreement.PublicKey(x963Representation: recipientKeyBytesValue.data),
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

    public func sign(input: KotlinByteArray) async throws -> KmmResult<KotlinByteArray> {
        guard let privateKey = keyChainService.loadPrivateKey() else {
            return KmmResultFailure(KotlinThrowable(message: "Could not load private key"))
        }
        guard let signature = try? privateKey.signature(for: input.data) else {
            return KmmResultFailure(KotlinThrowable(message: "Signature error"))
        }
        return KmmResultSuccess(signature.derRepresentation.kotlinByteArray)
    }

    public func toPublicKey() -> CryptoPublicKey {
        return cryptoPublicKey
    }

}

public class VcLibVerifierCryptoService : VerifierCryptoService {

    public func verify(input: KotlinByteArray, signature: KotlinByteArray, algorithm: JwsAlgorithm, publicKey: CryptoPublicKey) -> KmmResult<KotlinBoolean> {
        if algorithm != .es256 {
            return KmmResultFailure(KotlinThrowable(message: "Can not verify algorithm \(algorithm.name)"))
        }
        guard let publicKey = publicKey as? CryptoPublicKey.Ec else {
            return KmmResultFailure(KotlinThrowable(message: "Public key is not an EC key \(publicKey)"))
        }
        let ansiX963Result = publicKey.toAnsiX963ByteArray()
        if let throwable = ansiX963Result.exceptionOrNull() {
            return KmmResultFailure(throwable)
        }
        guard let publicKeyBytes = ansiX963Result.getOrNull(),
            let cryptoKitPublicKey = try? P256.Signing.PublicKey(x963Representation: publicKeyBytes.data) else {
            return KmmResultFailure(KotlinThrowable(message: "Can not create CryptoKit key"))
        }
        if let cryptoKitSignature = try? P256.Signing.ECDSASignature(derRepresentation: signature.data) {
            let valid = cryptoKitPublicKey.isValidSignature(cryptoKitSignature, for: input.data)
            return KmmResultSuccess(KotlinBoolean(value: valid))
        } else if let cryptoKitSignature = try? P256.Signing.ECDSASignature(rawRepresentation: signature.data) {
            let valid = cryptoKitPublicKey.isValidSignature(cryptoKitSignature, for: input.data)
            return KmmResultSuccess(KotlinBoolean(value: valid))
        } else {
            return KmmResultFailure(KotlinThrowable(message: "Can not read signature"))
        }
    }

    public func extractPublicKeyFromX509Cert(it: KotlinByteArray) -> JsonWebKey? {
        guard let certificate = SecCertificateCreateWithData(nil, it.data as CFData),
              let publicKey = SecCertificateCopyKey(certificate),
              let publicKeyData = SecKeyCopyExternalRepresentation(publicKey, nil) as? Data else {
            return nil
        }
        return JsonWebKey.companion.fromAnsiX963Bytes(type: .ec, curve: .secp256R1, it: publicKeyData.kotlinByteArray)
    }

}

public class VcLibEphemeralKeyHolder : EphemeralKeyHolder {

    let privateKey: P256.KeyAgreement.PrivateKey
    let publicKey: P256.KeyAgreement.PublicKey
    let jsonWebKey: JsonWebKey

    public init() {
        self.privateKey = P256.KeyAgreement.PrivateKey()
        self.publicKey = privateKey.publicKey
        self.jsonWebKey = JsonWebKey.companion.fromAnsiX963Bytes(type: .ec, curve: .secp256R1, it: publicKey.x963Representation.kotlinByteArray)!
    }

    public func toPublicJsonWebKey() -> JsonWebKey {
        return jsonWebKey
    }

}
