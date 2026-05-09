import UIKit
import SwiftUI
import shared

private func makeBuildContext() -> BuildContext {
    #if DEBUG
    let buildType = BuildType.debug
    #else
    let buildType = BuildType.release_
    #endif
    // CFBundleVersion is stored as a String in Info.plist; casting directly to Int32 always
    // yields nil on modern Xcode builds.
    return BuildContext(
        buildType: buildType,
        packageName: Bundle.main.bundleIdentifier ?? "at.asitplus.wallet.compose",
        versionCode: (Bundle.main.infoDictionary?["CFBundleVersion"] as? String).flatMap { Int32($0) } ?? 1,
        versionName: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0",
        osVersion: "iOS " + UIDevice.current.systemVersion
    )
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        Main_iosKt.MainViewController(buildContext: makeBuildContext())
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

// Wraps TransientFlowMainViewController for URL-based and DC API flows.
// Presented as a sheet over ContentView, mirroring Android's TransientFlowActivity.
struct TransientFlowView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        Main_iosKt.TransientFlowMainViewController(buildContext: makeBuildContext())
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @State private var showTransientFlow = false

    var body: some View {
        ComposeView()
            .ignoresSafeArea(.all)
            .onOpenURL { url in
                IosSessionBridge.shared.handleIncomingUrl(url: url.absoluteString) {
                    showTransientFlow = false
                }
                showTransientFlow = true
            }
            .sheet(isPresented: $showTransientFlow) {
                TransientFlowView()
                    .ignoresSafeArea(.all)
            }
    }
}
