import UIKit
import SwiftUI
import shared

struct ComposeView: UIViewControllerRepresentable {
    
    func makeUIViewController(context: Context) -> UIViewController {
        return Main_iosKt.MainViewController(objectFactory: SwiftObjectFactory(), platformAdapter: SwiftPlatformAdapter())
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
                .ignoresSafeArea(.keyboard) // Compose has own keyboard handler
                .onOpenURL { url in
                    AppKt.appLink.setValue(url.absoluteString)
                }
    }
}

class SwiftPlatformAdapter: PlatformAdapter {
    func openUrl(url: String){
        if let uri = URL(string: url) {
            UIApplication.shared.open(uri)
        }
    }
    
    func decodeImage(image: KotlinByteArray) -> Ui_graphicsImageBitmap {
        return IosUtilities.init().decodeImage(image: image)
    }
}

class SwiftObjectFactory: ObjectFactory {
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

