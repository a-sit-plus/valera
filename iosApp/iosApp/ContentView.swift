import UIKit
import SwiftUI
import shared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        return Main_iosKt.MainViewController(cryptoServiceSupplier: {
            let keyChainService = RealKeyChainService()
            do {
                try keyChainService.generateKeyPair()
            } catch {
                NapierProxy.companion.e(msg: "Error from keyChainService.generateKeyPair")
            }
            return VcLibCryptoServiceCryptoKit(keyChainService: keyChainService)!
        })
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
                .ignoresSafeArea(.keyboard) // Compose has own keyboard handler
    }
}



