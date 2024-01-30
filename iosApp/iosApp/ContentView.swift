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
        DispatchQueue.main.async {
            if let uri = URL(string: url) {
                UIApplication.shared.open(uri)
            }
        }
        
    }
    
    func decodeImage(image: KotlinByteArray) -> Ui_graphicsImageBitmap {
        return IosUtilities.init().decodeImage(image: image)
    }

    func writeToFile(text: String, fileName: String) {
        if let url = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first {
            let fileUrl = url.appendingPathComponent(fileName)
            if let file = try? FileHandle(forWritingTo: fileUrl) {
                file.seekToEndOfFile()
                file.write(text.data(using: .utf8)!)
                file.closeFile()
            } else {
                do {
                    try text.data(using: .utf8)?.write(to: fileUrl)
                } catch {
                    NapierProxy.companion.d(msg: "Unable to write to file: \(fileName)")
                }
            }
        }
    }
    func readFromFile(fileName: String) -> String? {
        if let url = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first {
            let fileUrl = url.appendingPathComponent(fileName)
            do {
                let log = try String(contentsOf: fileUrl, encoding: .utf8)
                return log
            } catch {
                NapierProxy.companion.d(msg: "Unable to read from file: \(fileName)")
                return nil
            }
        }
        return nil
    }

    func clearFile(fileName: String) {
        if let url = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first {
            let fileUrl = url.appendingPathComponent(fileName)
            do {
                try FileManager().removeItem(at: fileUrl)
            } catch {
                
            }
        }
    }

    func exitApp() {
        NapierProxy.companion.d(msg: "Exit App gracefully")
        exit(0)
    }

    func shareLog() {
            if let url = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first {
                let fileUrl = url.appendingPathComponent("log.txt") as NSURL
                let currentController = UIApplication.shared.windows.first(where: \.isKeyWindow)?.rootViewController
                let activityViewController = UIActivityViewController(activityItems: [fileUrl], applicationActivities: nil)
                currentController?.present(activityViewController, animated: true, completion: {})
            }
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

