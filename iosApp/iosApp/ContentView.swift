import UIKit
import SwiftUI
import shared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let keyChainService = RealKeyChainService()
        do {
            try keyChainService.generateKeyPair()
        } catch {
            NapierProxy.companion.e(msg: "Error from keyChainService.generateKeyPair")
        }
        let cryptoService = VcLibCryptoServiceCryptoKit(keyChainService: keyChainService)!
        return Main_iosKt.MainViewController(cryptoService: cryptoService)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
                .ignoresSafeArea(.keyboard) // Compose has own keyboard handler
    }
}



