import UIKit
import SwiftUI
import shared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        var holderKeyService: HolderKeyService?
        do {
            holderKeyService = try RealKeyChainService()
        } catch {
            
        }
        return Main_iosKt.MainViewController(objectFactory: SwiftObjectFactory(), holderKeyService: holderKeyService)
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
    var dataStoreService: DataStoreService?

    func loadCryptoService() -> KmmResult<CryptoService> {

        do {
            let keyChainService = try RealKeyChainService()
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
    func clear(){
        do {
            let keyChainService = try RealKeyChainService()
            keyChainService.clear()
        }
        catch {
            NapierProxy.companion.e(msg: "Error on clearing key")
        }
    }

}

