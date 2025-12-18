import ExtensionKit
import IdentityDocumentServicesUI
import IdentityDocumentServices
import SwiftUI
import shared

class StatefulViewController: UIViewController {
    var current: UIViewController?

    override func viewDidLoad() {
        super.viewDidLoad()
    }

    func display(viewController: UIViewController) {
        if let current = self.current {
            current.willMove(toParent: nil)
            current.view.removeFromSuperview()
            current.removeFromParent()
        }
        
        self.current = viewController
        
        self.addChild(viewController)
        self.view.addSubview(viewController.view)
        
        viewController.view.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            viewController.view.leadingAnchor.constraint(equalTo: self.view.leadingAnchor),
            viewController.view.trailingAnchor.constraint(equalTo: self.view.trailingAnchor),
            viewController.view.topAnchor.constraint(equalTo: self.view.topAnchor),
            viewController.view.bottomAnchor.constraint(equalTo: self.view.bottomAnchor)
        ])
        
        viewController.didMove(toParent: self)
    }
}

@main
struct DocumentProviderExtension: IdentityDocumentProvider {

    struct RootViewController: UIViewControllerRepresentable {
        let requestContext: ISO18013MobileDocumentRequestContext

        #if DEBUG
        let buildType = BuildType.debug
        #else
        let buildType = BuildType.release_
        #endif
        
        class Coordinator {
            var onSendResponseCalled = false
        }

        func makeCoordinator() -> Coordinator {
            Napier.shared.log(priority: LogLevel.debug, tag: "DocumentProviderExtension", throwable: nil, message: "makeCoordinator called")
            return Coordinator()
        }

        func makeUIViewController(context: Context) -> StatefulViewController {
            Napier.shared.base(antilog:OSLogNapierAntilog())
            Napier.shared.log(priority: LogLevel.debug, tag: "DocumentProviderExtension", throwable: nil, message: "makeUIViewController called")

            let mainViewController = Main_iosKt.MainViewController(
                buildContext: BuildContext(
                    buildType: buildType,
                    packageName: Bundle.main.bundleIdentifier ?? "at.asitplus.wallet.compose",
                    versionCode: Bundle.main.infoDictionary?["CFBundleVersion"] as? Int32 ?? 1,
                    versionName: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String  ?? "1.0.0",
                    osVersion: "iOS " + UIDevice.current.systemVersion
                ),
            )

            let statefulViewController = StatefulViewController()
            
            let originString: String? = requestContext.requestingWebsiteOrigin?.absoluteString
            
            var requestedElements: [String] = []
            if let presentmentRequest = requestContext.request.presentmentRequests.first {
                for docRequestSet in presentmentRequest.documentRequestSets {
                    for docRequest in docRequestSet.requests {
                        for (_, elements) in docRequest.namespaces {
                            for (elementIdentifier, _) in elements {
                                requestedElements.append(elementIdentifier)
                            }
                        }
                    }
                }
            }

            let onSendResponse: (Data) -> Void = { payload in
                if context.coordinator.onSendResponseCalled {
                    Napier.shared.log(priority: LogLevel.debug, tag: "DocumentProviderExtension", throwable: nil, message: "onSendResponse already called, ignoring")
                    return
                }
                context.coordinator.onSendResponseCalled = true

                Napier.shared.log(priority: LogLevel.debug, tag: "DocumentProviderExtension", throwable: nil, message: "onSendResponse called")
                Task {
                    do {
                        try await requestContext.sendResponse { rawRequest in
                            Napier.shared.log(priority: LogLevel.debug, tag: "DocumentProviderExtension", throwable: nil, message: "sendResponse handler started")
                            Napier.shared.log(priority: LogLevel.debug, tag: "DocumentProviderExtension", throwable: nil, message: "rawRequest: \(String(decoding: rawRequest.requestData, as: UTF8.self))")
                            let finalResponseData = await withCheckedContinuation { continuation in
                                Napier.shared.log(priority: LogLevel.debug, tag: "DocumentProviderExtension", throwable: nil, message: "withCheckedContinuation started")
                                let onFinish: (Data?) -> Void = { data in
                                    Napier.shared.log(priority: LogLevel.debug, tag: "DocumentProviderExtension", throwable: nil, message: "onFinish called")
                                    continuation.resume(returning: data ?? Data())
                                }
                                
                                MdocSessionManager.shared.setSession(request: rawRequest.requestData, origin: originString, callback: onFinish)
                                
                                Napier.shared.log(priority: LogLevel.debug, tag: "DocumentProviderExtension", throwable: nil, message: "Before displaying MainViewController")

                                DispatchQueue.main.async {
                                    Napier.shared.log(priority: LogLevel.debug, tag: "DocumentProviderExtension", throwable: nil, message: "Displaying MainViewController")
                                    statefulViewController.display(viewController: mainViewController)
                                }
                            }

                            
                            Napier.shared.log(priority: LogLevel.debug, tag: "DocumentProviderExtension", throwable: nil, message: "sendResponse handler finished")
                            let response = ISO18013MobileDocumentResponse(responseData: finalResponseData)
                            return response
                        }
                    } catch {
                        Napier.shared.log(priority: LogLevel.error, tag: "DocumentProviderExtension", throwable: nil, message: "sendResponse failed: \(error)")
                        requestContext.cancel()
                    }
                }
            }

            let onCancel: () -> Void = {
                Napier.shared.log(priority: LogLevel.debug, tag: "DocumentProviderExtension", throwable: nil, message: "onCancel called")
                requestContext.cancel()
            }

            let mdocRequestViewController = Main_iosKt.MdocRequestViewController(
                requestingWebsiteOrigin: originString,
                requestedElements: requestedElements,
                onSendResponse: onSendResponse,
                onCancel: onCancel
            )
            
            statefulViewController.display(viewController: mdocRequestViewController)

            return statefulViewController
        }

        func updateUIViewController(_ uiViewController: StatefulViewController, context: Context) {
        }
    }

    var body: some IdentityDocumentRequestScene {
        ISO18013MobileDocumentRequestScene { context in
            Napier.shared.log(priority: LogLevel.debug, tag: "DocumentProviderExtension", throwable: nil, message: "ISO18013MobileDocumentRequestScene called")
            return RootViewController(requestContext: context)
        }
    }

    func performRegistrationUpdates() async {
    }
}