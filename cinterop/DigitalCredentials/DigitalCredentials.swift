import Foundation
import IdentityDocumentServices

@objc(DigitalCredentials) public class DigitalCredentials: NSObject {

    @objc public class func storeDocument(id: String, docType: String) async -> Bool {
        let store = IdentityDocumentProviderRegistrationStore()

        do {
            let registration = MobileDocumentRegistration(
                mobileDocumentType: docType,
                supportedAuthorityKeyIdentifiers: [],
                documentIdentifier: id
            )

            try await store.addRegistration(registration)
        } catch {
            return false
        }

        return true
    }
}
