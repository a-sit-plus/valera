import Foundation
import IdentityDocumentServices

@objc(DigitalCredentials) public class DigitalCredentials: NSObject {
    private static func registrationErrorMessage(for error: Error) -> String {
        guard let registrationError = error as? IdentityDocumentProviderRegistrationStore.RegistrationError else {
            return error.localizedDescription
        }

        switch registrationError {
        case .unknown:
            return "unknown"
        case .invalidRequest:
            return "invalidRequest"
        case .notAuthorized:
            return "notAuthorized"
        case .notSupported:
            return "notSupported"
        @unknown default:
            return String(describing: registrationError)
        }
    }

    @objc public class func storeDocument(id: String, docType: String) async -> String? {
        let store = IdentityDocumentProviderRegistrationStore()

        do {
            let registration = MobileDocumentRegistration(
                mobileDocumentType: docType,
                supportedAuthorityKeyIdentifiers: [],
                documentIdentifier: id
            )

            try await store.addRegistration(registration)
        } catch {
            return registrationErrorMessage(for: error)
        }

        return nil
    }
}
