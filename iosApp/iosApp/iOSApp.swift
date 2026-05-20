import SwiftUI
import shared

@main
struct iOSApp: App {
    #if DEBUG
    private let buildType = BuildType.debug
    #else
    private let buildType = BuildType.release_
    #endif

    init() {
        IosSessionBridge.shared.bootstrap(
            buildContext: BuildContext(
                buildType: buildType,
                packageName: Bundle.main.bundleIdentifier ?? "at.asitplus.wallet.compose",
                versionCode: (Bundle.main.infoDictionary?["CFBundleVersion"] as? String).flatMap { Int32($0) } ?? 1,
                versionName: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0",
                osVersion: "iOS " + UIDevice.current.systemVersion
            ),
            antilog: OSLogNapierAntilog()
        )
    }

	var body: some Scene {
		WindowGroup {
			ContentView()
		}
	}
}
