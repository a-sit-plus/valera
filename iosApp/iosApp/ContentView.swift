import UIKit
import SwiftUI
import shared

var objectFactory = SwiftObjectFactory()

struct ComposeView: UIViewControllerRepresentable {
    
    func makeUIViewController(context: Context) -> UIViewController {
        return Main_iosKt.MainViewController(objectFactory: objectFactory)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
                .ignoresSafeArea(.keyboard) // Compose has own keyboard handler
                .onOpenURL { url in
                    objectFactory.appLink = url.absoluteString
                }
    }
}

class SwiftObjectFactory: ObjectFactory {
    var appLink: String? // Currently the only way to pass the url to kotlin multiplatform
    
    lazy var keyChainService: RealKeyChainService = {RealKeyChainService()}()
    
    func loadCryptoService() -> KmmResult<CryptoService> {

        do {
            try keyChainService.initialize()
            guard let cryptoService = VcLibCryptoServiceCryptoKit(keyChainService: keyChainService) else {
                NapierProxy.companion.e(msg: "Error on creating VcLibCryptoServiceCryptoKit")
                return KmmResultFailure(KotlinThrowable(message: "Error on creating VcLibCryptoServiceCryptoKit"))
            }
            return KmmResultSuccess(cryptoService)
        } catch {
            NapierProxy.companion.e(msg: "Error from keyChainService.generateKeyPair")
            return KmmResultFailure(KotlinThrowable(message: "Error from keyChainService.generateKeyPair"))
        }

    }

    func loadHolderKeyService() -> KmmResult<HolderKeyService> {
        return KmmResultSuccess(keyChainService)
    }

}

