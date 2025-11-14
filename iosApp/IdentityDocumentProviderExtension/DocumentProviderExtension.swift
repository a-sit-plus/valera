import ExtensionKit
import IdentityDocumentServicesUI
import SwiftUI

@main
struct DocumentProviderExtension: IdentityDocumentProvider {


    var body: some IdentityDocumentRequestScene {
        ISO18013MobileDocumentRequestScene { context in
            // Insert your view here
            Text("Hello, world!")
            /*WindowGroup {
                ContentView()
            }*/
        }
    }

    func performRegistrationUpdates() async {
        
    }

}
