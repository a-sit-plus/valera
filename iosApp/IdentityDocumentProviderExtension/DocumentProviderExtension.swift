import ExtensionKit
import IdentityDocumentServicesUI
import IdentityDocumentServices
import SwiftUI
import shared

@main
struct DocumentProviderExtension: IdentityDocumentProvider {

    struct ComposeViewController: UIViewControllerRepresentable {
        // Tunnel ISO18013 scene context into the UIKit bridge
        let requestContext: ISO18013MobileDocumentRequestContext

        #if DEBUG
        let buildType = BuildType.debug
        #else
        let buildType = BuildType.release_
        #endif

        // TODO finish trying with most basic UI
        func makeUIViewController(context: Context) -> UIViewController {
            Napier.shared.base(antilog:OSLogNapierAntilog())
            Napier.shared.log(priority: LogLevel.debug, tag: "123", throwable: nil, message: "HIER!!!!!!!!!!!!!!!!!!!!!!!!!33")
            
            // Extract origin URL as String for Kotlin
            let originString: String? = requestContext.requestingWebsiteOrigin?.absoluteString

            // Raw request payload is only available inside sendResponse(responseHandler:),
            // so for now we pass nil here. We'll surface it when we get it in the response handler.
            let requestPayload: Data? = nil
            let requestDescription: String? = nil

            // Bridge callbacks that Kotlin can call to answer/cancel
            let onSendResponse: (Data) -> Void = { payload in
                // Kick off the platform flow: we provide a handler that receives the raw request,
                // and we return an ISO18013MobileDocumentResponse built from Kotlin-provided bytes.
                Task {
                    do {
                        try await requestContext.sendResponse { rawRequest in
                            // If Kotlin needs to see the request bytes first, we can extend the bridge to pass
                            // rawRequest.requestData to Kotlin and await a response; for now, we already have it.
                            let response = ISO18013MobileDocumentResponse(responseData: payload)
                            return response
                        }
                    } catch {
                        // If sending a response fails, cancel the session as a fallback
                        requestContext.cancel()
                    }
                }
            }

            let onCancel: () -> Void = {
                requestContext.cancel()
            }

            // Try to construct the Compose-based UI, but fall back to a Swift UI if it fails
            /*if let vc = Main_iosKt.tryComposeEntryPoint(
                requestingWebsiteOrigin: originString,
                requestPayload: requestPayload,
                requestDescription: requestDescription,
                onSendResponse: onSendResponse,
                onCancel: onCancel
            ) {
                return vc
            } else {
                return makeSwiftFallbackViewController()
            }*/
            //Main_iosKt.doInitLogger(isDebug: true)

            
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

        func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        }

        // MARK: - Swift fallback UI
        private func makeSwiftFallbackViewController() -> UIViewController {
            let vc = UIViewController()
            vc.view.backgroundColor = .systemBackground

            let label = UILabel()
            label.text = "Unable to open request UI."
            label.textAlignment = .center
            label.numberOfLines = 0

            let button = UIButton(type: .system)
            button.setTitle("Close", for: .normal)
            // Use UIAction to avoid needing an @objc selector on a struct
            button.addAction(UIAction { _ in
                requestContext.cancel()
            }, for: .touchUpInside)

            let stack = UIStackView(arrangedSubviews: [label, button])
            stack.axis = .vertical
            stack.alignment = .center
            stack.spacing = 16

            vc.view.addSubview(stack)
            stack.translatesAutoresizingMaskIntoConstraints = false
            NSLayoutConstraint.activate([
                stack.centerXAnchor.constraint(equalTo: vc.view.centerXAnchor),
                stack.centerYAnchor.constraint(equalTo: vc.view.centerYAnchor),
                stack.leadingAnchor.constraint(greaterThanOrEqualTo: vc.view.leadingAnchor, constant: 20),
                vc.view.trailingAnchor.constraint(greaterThanOrEqualTo: stack.trailingAnchor, constant: 20)
            ])

            return vc
        }
        // No @objc selector needed; handled via UIAction above.
    }


    var body: some IdentityDocumentRequestScene {
        ISO18013MobileDocumentRequestScene { context in
            // Insert your view here
            //Text("Hello, world!")
            //WindowGroup {
            // Pass the scene context down to the UIKit representable
            ComposeViewController(requestContext: context)
            //}
        }
    }

    func performRegistrationUpdates() async {
        
    }

}
