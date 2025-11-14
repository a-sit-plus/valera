import Foundation
import IdentityDocumentServices

@objc(DigitalCredentials) public class DigitalCredentials: NSObject {

    @objc public class func storeDocument(id: String, docType: String) async -> Bool {
        let store = IdentityDocumentProviderRegistrationStore()

        do {
            let registration = MobileDocumentRegistration(
                mobileDocumentType: docType,
                supportedAuthorityKeyIdentifiers: [],
                documentIdentifier: id//,
                //invalidationDate: storedDocument.invalidationDate
            )

            try await store.addRegistration(registration)
        } catch {
            return false
        }

        return true
    }

    /*@objc public func removeDocuments(_ documentIds: [String]) -> Boolean {
        let store = IdentityDocumentProviderRegistrationStore()

        do {
            let storedRegistrations = try await store.registrations

            let matchingRegistration = storedRegistrations.first { storedRegistration in
                // Your app logic to determine which registration to remove.
            }

            guard let documentIdentifier = matchingRegistration?.documentIdentifier else {
                // Throw an error.
            }

            try await registrationStore.removeRegistration(forDocumentIdentifier: documentIdentifier)
        } catch {
            // Handle the error.
            return false
        }
        return true
    }*/
}
