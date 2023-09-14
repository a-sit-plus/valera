import UIKit
import SwiftUI
import shared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        return Main_iosKt.MainViewController(objectFactory: SwiftObjectFactory())
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
                .ignoresSafeArea(.keyboard) // Compose has own keyboard handler
    }
}

class SwiftObjectFactory: ObjectFactory {

    func loadCryptoService() async throws -> CryptoService {
        let keyChainService = RealKeyChainService()
        do {
            try await keyChainService.generateKeyPair()
        } catch {
            NapierProxy.companion.e(msg: "Error from keyChainService.generateKeyPair")
        }
        return VcLibCryptoServiceCryptoKit(keyChainService: keyChainService)!
    }

}

