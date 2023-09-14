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

    func loadCryptoService() async throws -> KmmResult<CryptoService> {
        let keyChainService = RealKeyChainService()
        do {
            try await keyChainService.generateKeyPair()
        } catch {
            NapierProxy.companion.e(msg: "Error from keyChainService.generateKeyPair")
            return KmmResultFailure(KotlinThrowable(message: "Error from keyChainService.generateKeyPair"))
        }
        guard let cryptoService = VcLibCryptoServiceCryptoKit(keyChainService: keyChainService) else {
            NapierProxy.companion.e(msg: "Error on creating VcLibCryptoServiceCryptoKit")
            return KmmResultFailure(KotlinThrowable(message: "Error on creating VcLibCryptoServiceCryptoKit"))
        }
        return KmmResultSuccess(cryptoService)
    }

}

