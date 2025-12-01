import UIKit
import SwiftUI
import shared

struct ComposeView: UIViewControllerRepresentable {
    
    func makeUIViewController(context: Context) -> UIViewController {
        //Main_iosKt.doInitLogger(isDebug: true)
        
        Napier.shared.base(antilog:OSLogNapierAntilog())
        Napier.shared.log(priority: LogLevel.debug, tag: "123", throwable: nil, message: "HIER!!!!!!!!!!!!!!!!!!!!!!!!!33")

        #if DEBUG
        let buildType = BuildType.debug
        #else
        let buildType = BuildType.release_
        #endif
        return Main_iosKt.MainViewController(
            buildContext: BuildContext(
                buildType: buildType,
                packageName: Bundle.main.bundleIdentifier ?? "at.asitplus.wallet.compose",
                versionCode: Bundle.main.infoDictionary?["CFBundleVersion"] as? Int32 ?? 1,
                versionName: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String  ?? "1.0.0",
                osVersion: "iOS " + UIDevice.current.systemVersion
            )
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
                .ignoresSafeArea(.all)
                .onOpenURL { url in
                    Globals.shared.appLink.setValue(url.absoluteString)
                }
    }
}

