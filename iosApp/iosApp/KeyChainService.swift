import Foundation
import DeviceCheck
import LocalAuthentication
import CryptoKit
import shared

public protocol KeyChainService {
    func generateKeyPair() async throws
    func loadPrivateKey() -> SecureEnclave.P256.Signing.PrivateKey?
    func loadPrivateKey(authContext: LAContext?) -> SecureEnclave.P256.Signing.PrivateKey?
    func loadPublicKey() -> P256.Signing.PublicKey?
    func loadPublicKeyData() -> Data?
    func attestKey(with challenge: Data, also clientData: Data) async -> [Data]?
    func add(attestedPublicKey: String) throws
    func loadAttestedPublicKey() -> String?
    func sign(data: Data, using key: SecureEnclave.P256.Signing.PrivateKey) throws -> Data
    func clear()
    func authenticateUser(_ detailText: String) async -> LAContext?
}

extension String: Error {}

public class RealKeyChainService : KeyChainService, HolderKeyService {
    private static let KEY_PAIR_ALIAS = "bindingKey"
    private static let BINDING_CERT_ALIAS = "bindingCert"
    private static let ATTESTED_PUBLIC_KEY_ALIAS = "attestedPublicKey"
    private static let ACCOUNT = "wallet"
    private var attestationKeyId: String?
    private var cachedAuthContext: LAContext?
    private var privateKey: SecureEnclave.P256.Signing.PrivateKey?
    private var publicKey: P256.Signing.PublicKey?

    public func initialize() throws {
        self.privateKey = loadPrivateKey()
        self.publicKey = loadPublicKey()
        if (self.privateKey == nil) {
            Task {
                try await generateKeyPair()
            }
        }
    }

    public func attestKey(with challenge: Data, also clientData: Data) async -> [Data]? {
        if DCAppAttestService.shared.isSupported,
            let attestationKeyId = attestationKeyId {
            guard let attestation = try? await DCAppAttestService.shared.attestKey(
                    attestationKeyId, clientDataHash: Data(SHA256.hash(data: challenge))) else {
                NapierProxy.companion.e(msg: "attestKey: Error creating attestation")
                return nil
            }
            guard let assertion = try? await DCAppAttestService.shared.generateAssertion(
                    attestationKeyId, clientDataHash: Data(SHA256.hash(data: clientData))) else {
                NapierProxy.companion.e(msg: "attestKey: Error creating assertion")
                return nil
            }
            return [attestation, assertion]
        }
        return nil
    }

    public func generateKeyPair() async throws {
        NapierProxy.companion.i(msg: "generateKeyPair")
        clear()
        /*guard let authContext = await authenticateUser(String("Create new key pair")) else {
            NapierProxy.companion.e(msg: "generateKeyPair: Cannot authenticate user")
            throw "user auth failed"
        }*/

        let flags: SecAccessControlCreateFlags = [.privateKeyUsage]//, .userPresence]
        let authContext : LAContext? = nil
        var error: Unmanaged<CFError>?
        guard let access = SecAccessControlCreateWithFlags(kCFAllocatorDefault, kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly, flags, &error) else {
            NapierProxy.companion.e(msg: "generateKeyPair: Cannot create key access flags: \(error.debugDescription)")
            throw "cannot create key access flags"
        }

        guard let privateKey = try? SecureEnclave.P256.Signing.PrivateKey(compactRepresentable: true, accessControl: access, authenticationContext: authContext) else {
            NapierProxy.companion.e(msg: "generateKeyPair: Can not create SecureEnclave key")
            throw "Can not create SecureEnclave key"
        }
        self.privateKey = privateKey
        self.publicKey = privateKey.publicKey

        // SecureEnclave keys from CryptoKit shall be stored as "passwords"
        // (their data representation is an encrypted blob)
        let query = [kSecClass: kSecClassGenericPassword,
                     kSecAttrAccessible: kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly,
                     kSecUseDataProtectionKeychain: true,
                     kSecAttrLabel: RealKeyChainService.KEY_PAIR_ALIAS,
                     kSecAttrAccount: RealKeyChainService.ACCOUNT,
                     kSecValueData: privateKey.dataRepresentation] as [String: Any]

        let status = SecItemAdd(query as CFDictionary, nil)
        guard status == errSecSuccess else {
            NapierProxy.companion.e(msg: "generateKeyPair: Unable to store item: \(status)")
            throw "unable to store item: \(status)"
        }

        if DCAppAttestService.shared.isSupported {
            guard let keyId = try? await DCAppAttestService.shared.generateKey() else {
                NapierProxy.companion.e(msg: "generateKeyPair: App Attest Service cannot generate keypair")
                throw "cannot attest key pair"
            }
            attestationKeyId = keyId
        }
    }

    public func clear() {
        clearKeychain(for: RealKeyChainService.KEY_PAIR_ALIAS)
        clearKeychain(for: RealKeyChainService.BINDING_CERT_ALIAS)
        clearKeychain(for: RealKeyChainService.ATTESTED_PUBLIC_KEY_ALIAS)
        self.privateKey = nil
        self.publicKey = nil
    }

    private func clearKeychain(for alias: String) {
        clearCertificate(for: alias)
        clearGenericPassword(for: alias)
    }

    public func loadPrivateKey() -> SecureEnclave.P256.Signing.PrivateKey? {
        return loadPrivateKey(authContext: nil)
    }

    public func loadPrivateKey(authContext: LAContext? = nil) -> SecureEnclave.P256.Signing.PrivateKey? {
        NapierProxy.companion.d(msg: "loadPrivateKey")
        if let privateKey = self.privateKey {
            guard let privateKey = try? SecureEnclave.P256.Signing.PrivateKey(dataRepresentation: privateKey.dataRepresentation, authenticationContext: authContext ?? cachedAuthContext ?? nil) else {
                NapierProxy.companion.e(msg: "loadPrivateKey: Cannot reconstruct CryptoKit key")
                return nil
            }
            return privateKey
        }
        var query = [kSecClass: kSecClassGenericPassword,
                     kSecUseDataProtectionKeychain: true,
                     kSecAttrLabel: RealKeyChainService.KEY_PAIR_ALIAS,
                     kSecAttrAccount: RealKeyChainService.ACCOUNT,
                     kSecReturnData: true] as [String: Any]
        if let authContext = authContext {
            query[kSecUseAuthenticationContext as String] = authContext
        }
        if let authContext = cachedAuthContext {
            query[kSecUseAuthenticationContext as String] = authContext
        }

        var item: CFTypeRef?
        switch SecItemCopyMatching(query as CFDictionary, &item) {
        case errSecSuccess:
            guard let data = item as? Data else {
                NapierProxy.companion.e(msg: "loadPrivateKey: Cannot decode data")
                return nil
            }
            guard let privateKey = try? SecureEnclave.P256.Signing.PrivateKey(dataRepresentation: data, authenticationContext: authContext ?? cachedAuthContext ?? nil) else {
                NapierProxy.companion.e(msg: "loadPrivateKey: Cannot reconstruct CryptoKit key")
                return nil
            }
            self.privateKey = privateKey
            return privateKey
        case errSecItemNotFound:
            NapierProxy.companion.w(msg: "loadPrivateKey: Keychain item not found")
            return nil
        case let status:
            NapierProxy.companion.e(msg: "loadPrivateKey: Keychain read failed: \(status)")
            return nil
        }
    }

    public func loadPublicKey() -> P256.Signing.PublicKey? {
        if let publicKey = self.publicKey {
            return publicKey
        }
        if let privateKey = self.privateKey {
            self.publicKey = privateKey.publicKey
            return publicKey
        }
        if let privateKey = loadPrivateKey() {
            self.publicKey = privateKey.publicKey
            return publicKey
        }
        return nil
    }

    public func loadPublicKeyData() -> Data? {
        if let publicKey = loadPublicKey() {
            return publicKey.x963Representation
        }
        return nil
    }

    public func add(attestedPublicKey: String) throws {
        clearGenericPassword(for: RealKeyChainService.ATTESTED_PUBLIC_KEY_ALIAS)
        let query: [NSString: Any] = [
            kSecClass: kSecClassGenericPassword,
            kSecAttrLabel: RealKeyChainService.ATTESTED_PUBLIC_KEY_ALIAS,
            kSecValueData: attestedPublicKey.data(using: .utf8)!,
            kSecAttrAccessible: kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly,
        ]
        let status = SecItemAdd(query as CFDictionary, nil)
        guard status == errSecSuccess else {
            NapierProxy.companion.e(msg: "Could not add attested public key to keychain: status \(status)")
            throw "Could not add attested public key to keychain"
        }
    }

    public func loadAttestedPublicKey() -> String? {
        let query: [NSString: Any] = [
            kSecClass: kSecClassGenericPassword,
            kSecAttrLabel: RealKeyChainService.ATTESTED_PUBLIC_KEY_ALIAS,
            kSecReturnData: true,
        ]
        var ref : CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &ref)
        guard status == errSecSuccess,
              let key = ref as? Data else {
            NapierProxy.companion.e(msg: "Cannot load attested public key for alias \(RealKeyChainService.ATTESTED_PUBLIC_KEY_ALIAS) from keychain")
            return nil
        }
        return String(data: key, encoding: .utf8)
    }

    public func sign(data: Data, using key: SecureEnclave.P256.Signing.PrivateKey) throws -> Data {
        guard let signatureValue = try? key.signature(for: data) else {
            NapierProxy.companion.e(msg: "sign: error")
            throw "unexpected"
        }
        return signatureValue.derRepresentation
    }

    private func addCertificateToKeyChain(for alias: String, secCertificateData: CFData) throws {
        clearCertificate(for: alias)

        let certificate = SecCertificateCreateWithData(nil, secCertificateData)!
        let query: [NSString: Any] = [
            kSecClass: kSecClassCertificate,
            kSecAttrLabel: alias,
            kSecValueRef: certificate,
            kSecAttrAccessible: kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly,
        ]
        let status = SecItemAdd(query as CFDictionary, nil)

        guard status == errSecSuccess else {
            NapierProxy.companion.e(msg: "addCertificateToKeyChain: error \(status)")
            throw "unexpected"
        }
    }

    private func loadCertificateFromKeyChain(for alias: String) -> Data? {
        let query: [NSString: Any] = [
            kSecClass: kSecClassCertificate,
            kSecAttrLabel: alias,
            kSecReturnData: true,
        ]
        var ref : CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &ref)
        guard status == errSecSuccess,
              let cert = ref else {
            NapierProxy.companion.e(msg: "Cannot load certificate for alias \(alias) in keychain")
            return nil
        }
        return (cert as! Data)
    }

    public func authenticateUser(_ detailText: String) async -> LAContext? {
        if cachedAuthContext != nil {
            return cachedAuthContext
        }
        let authContext = LAContext()
        if #available(iOS 15, *) {
            authContext.localizedFallbackTitle = String(localized: "auth_passcode")
        }
        let isSuccess = try? await authContext.evaluatePolicy(.deviceOwnerAuthentication, localizedReason: detailText)
        guard let isSuccess = isSuccess,
              isSuccess else {
            return nil
        }
        cachedAuthContext = authContext
        return authContext
    }

    private func clearCertificate(for alias: String) {
        let query: [NSString: Any] = [
            kSecClass: kSecClassCertificate,
            kSecAttrLabel: alias,
        ]
        _ = SecItemDelete(query as CFDictionary)
    }

    func clearGenericPassword(for alias: String) {
        let deleteQuery : [NSString: Any] = [
            kSecClass: kSecClassGenericPassword,
            kSecAttrLabel: alias,
        ]
        _ = SecItemDelete(deleteQuery as CFDictionary)
    }
}
