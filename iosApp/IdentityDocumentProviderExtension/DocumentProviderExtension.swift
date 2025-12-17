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

        func makeUIViewController(context: Context) -> StatefulViewController {
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
                Task {
                    do {
                        try await requestContext.sendResponse { rawRequest in
                            let mainViewController = Main_iosKt.MainViewController(
                                buildContext: BuildContext(
                                    buildType: buildType,
                                    packageName: Bundle.main.bundleIdentifier ?? "at.asitplus.wallet.compose",
                                    versionCode: Bundle.main.infoDictionary?["CFBundleVersion"] as? Int32 ?? 1,
                                    versionName: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String  ?? "1.0.0",
                                    osVersion: "iOS " + UIDevice.current.systemVersion
                                ),
                                request: rawRequest.requestData
                            )

                            DispatchQueue.main.async {
                                statefulViewController.display(viewController: mainViewController)
                            }

                            let response = ISO18013MobileDocumentResponse(responseData: payload)
                            return response
                        }
                    } catch {
                        requestContext.cancel()
                    }
                }
            }

            let onCancel: () -> Void = {
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
            RootViewController(requestContext: context)
        }
    }

    func performRegistrationUpdates() async {
    }
}