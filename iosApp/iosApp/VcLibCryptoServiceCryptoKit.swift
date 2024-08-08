import Foundation
import CryptoKit
import shared
import os

public class VcLibCryptoServiceCryptoKit: CryptoServiceAdapter {
    
    private let keyChainService: KeyChainService
    
    public init?(keyChainService: KeyChainService) {
        NapierProxy.companion.d(msg: "Init VcLibCryptoServiceCryptoKit")
        guard let privateKey = keyChainService.loadPrivateKey() else {
            return nil
        }
        self.keyChainService = keyChainService
        guard let publicKey = try? CryptoPublicKey.companion.fromIosEncoded(it: privateKey.publicKey.x963Representation.kotlinByteArray) else {
            NapierProxy.companion.w(msg: "Cannot convert publicKey")
            return nil
        }

        super.init(
            publicKey: publicKey,
            algorithm: .es256,
            coseKey: publicKey.toCoseKey(algorithm: nil, keyId: publicKey.coseKid).getOrThrow()!,
            jsonWebKey: publicKey.toJsonWebKey(keyId: publicKey.jwkId),
            certificate: nil
        )
    }
    
    private func loadPrivateKey() -> SecureEnclave.P256.Signing.PrivateKey? {
        if let prompt = super.currentAuthorizationContext as? IosCryptoServiceAuthorizationContext {
            return keyChainService.loadPrivateKey(authContext: prompt.contex)
        } else {
            return keyChainService.loadPrivateKey()
        }
    }

    public override func decrypt(key: KotlinByteArray, iv: KotlinByteArray, aad: KotlinByteArray, input: KotlinByteArray, authTag: KotlinByteArray, algorithm: JweEncryption) async throws -> KmmResult<KotlinByteArray> {
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

    public override func encrypt(key: KotlinByteArray, iv: KotlinByteArray, aad: KotlinByteArray, input: KotlinByteArray, algorithm: JweEncryption) -> KmmResult<AuthenticatedCiphertext> {
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

    public override func generateEphemeralKeyPair(ecCurve: ECCurve) -> KmmResult<EphemeralKeyHolder> {
        switch ecCurve {
        case .secp256R1:
            return KmmResultSuccess(VcLibEphemeralKeyHolder())
        default:
            return KmmResultFailure(KotlinThrowable(message: "ecCurve unknown \(ecCurve)"))
        }
    }

    public override func messageDigest(input: KotlinByteArray, digest: KmpCryptoDigest) -> KmmResult<KotlinByteArray> {
        switch digest {
        case .sha256:
            let digest = SHA256.hash(data: input.data)
            let data = Data(digest.compactMap { $0 })
            return KmmResultSuccess(data.kotlinByteArray)
        default:
            return KmmResultFailure(KotlinThrowable(message: "Digest unknown \(digest)"))
        }
    }

    public override func performKeyAgreement(ephemeralKey: EphemeralKeyHolder, recipientKey: JsonWebKey, algorithm: JweAlgorithm) -> KmmResult<KotlinByteArray> {
        switch algorithm.identifier {
        case "ECDH-ES":
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

    public override func performKeyAgreement(ephemeralKey: JsonWebKey, algorithm: JweAlgorithm) -> KmmResult<KotlinByteArray> {
        switch algorithm.identifier {
        case "ECDH-ES":
            guard let privateKey = self.loadPrivateKey() else {
                return KmmResultFailure(KotlinThrowable(message: "Could not load private key"))
            }
            let ephemeralKeyBytes = ephemeralKey.toCryptoPublicKey()
            if let throwable = ephemeralKeyBytes.exceptionOrNull() {
                return KmmResultFailure(throwable)
            }
            guard let recipientKeyBytesValue = ephemeralKeyBytes.getOrNull()?.iosEncoded,
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

    public override func sign(input: KotlinByteArray) async throws -> KmmResult<CryptoSignatureRawByteEncodable> {
        guard let privateKey = self.loadPrivateKey() else {
            return KmmResultFailure(KotlinThrowable(message: "Could not load private key"))
        }
        guard let signature = try? privateKey.signature(for: input.data) else {
            return KmmResultFailure(KotlinThrowable(message: "Signature error"))
        }

        // Cast is needed, because the generic Asn1Decodable interface declares the return type as Asn1Encodable, and the implementing classes fix the return type to a specific class
        // Apparently this gets lost when generating ObjC headers
        return KmmResultSuccess(try CryptoSignatureEC.companion.fromRawBytes(input: signature.rawRepresentation.kotlinByteArray))
    }


}

public class VcLibVerifierCryptoService : VerifierCryptoService {

    public var supportedAlgorithms: [X509SignatureAlgorithm] = [.es256]

    public func verify(input: KotlinByteArray, signature: CryptoSignature, algorithm: X509SignatureAlgorithm, publicKey: CryptoPublicKey) -> KmmResult<KotlinBoolean> {
        if algorithm != .es256 {
            return KmmResultFailure(KotlinThrowable(message: "Can not verify algorithm \(algorithm.name)"))
        }
        guard let cryptoKitPublicKey = try? P256.Signing.PublicKey(x963Representation: publicKey.iosEncoded.data) else {
            return KmmResultFailure(KotlinThrowable(message: "Can not create CryptoKit key"))
        }
        if let cryptoKitSignature = try? P256.Signing.ECDSASignature(derRepresentation: (signature as! CryptoSignatureRawByteEncodable).rawByteArray.data) {
            let valid = cryptoKitPublicKey.isValidSignature(cryptoKitSignature, for: input.data)
            return KmmResultSuccess(KotlinBoolean(value: valid))
        } else if let cryptoKitSignature = try? P256.Signing.ECDSASignature(rawRepresentation: (signature as! CryptoSignatureRawByteEncodable).rawByteArray.data) {
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
