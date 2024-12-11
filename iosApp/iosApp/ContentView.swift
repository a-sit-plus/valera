import UIKit
import SwiftUI
import shared

struct ComposeView: UIViewControllerRepresentable {
    
    func makeUIViewController(context: Context) -> UIViewController {
        #if DEBUG
        let buildType = "debug"
        #else
        let buildType = "release"
        #endif
        return Main_iosKt.MainViewController(
            platformAdapter: SwiftPlatformAdapter(),
            buildContext: BuildContext(
                buildType: buildType,
                versionCode: Bundle.main.infoDictionary?["CFBundleVersion"] as? Int32 ?? 1,
                versionName: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String  ?? "1.0.0"
            )
        )
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

    func writeToFile(text: String, fileName: String, folderName: String) {
        if let url = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first {
            let folderUrl = url.appendingPathComponent(folderName)
            if !FileManager.default.fileExists(atPath: folderUrl.path){
                createDirectory(folderName: folderName)
            }
            let fileUrl = folderUrl.appendingPathComponent(fileName)
            if let file = try? FileHandle(forWritingTo: fileUrl) {
                file.seekToEndOfFile()
                file.write(text.data(using: .utf8)!)
                file.closeFile()
            } else {
                FileManager.default.createFile(atPath: fileUrl.path, contents: text.data(using: .utf8))
            }
        }
    }
    func readFromFile(fileName: String, folderName: String) -> String? {
        if let url = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first {
            let folderUrl = url.appendingPathComponent(folderName)
            if !FileManager.default.fileExists(atPath: folderUrl.path){
                createDirectory(folderName: folderName)
            }
            let fileUrl = folderUrl.appendingPathComponent(fileName)
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

    func clearFile(fileName: String, folderName: String) {
        if let url = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first {
            let folderUrl = url.appendingPathComponent(folderName)
            if !FileManager.default.fileExists(atPath: folderUrl.path){
                createDirectory(folderName: folderName)
            }
            let fileUrl = folderUrl.appendingPathComponent(fileName)
            do {
                try FileManager().removeItem(at: fileUrl)
            } catch {
                NapierProxy.companion.d(msg: "Unable to clear file: \(fileName)")
            }
        }
    }

    func createDirectory(folderName: String) {
        if let url = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first {
            let folderUrl = url.appendingPathComponent(folderName)
            do {
                try FileManager.default.createDirectory(at: folderUrl, withIntermediateDirectories: true)
            } catch {
                NapierProxy.companion.d(msg: "Unable to create from directory: \(folderUrl)")
            }
        }
    }

    func shareLog() {
        if let url = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first {
            let folderUrl = url.appendingPathComponent("logs")
            let fileUrl = folderUrl.appendingPathComponent("log.txt") as NSURL
            
            DispatchQueue.main.async {
                let connectedScenes = UIApplication.shared.connectedScenes
                let uiWindowScene = connectedScenes.first as? UIWindowScene
                let currentController = uiWindowScene?.windows.first(where: \.isKeyWindow)?.rootViewController
                let activityViewController = UIActivityViewController(activityItems: [fileUrl], applicationActivities: nil)
                currentController?.present(activityViewController, animated: true, completion: {})
            }
        }
    }
}

