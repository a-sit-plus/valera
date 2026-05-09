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

private struct ParsedRequestSummaryData {
    let summaryJson: String?
}

private func buildParsedRequestSummaryData(from requestContext: ISO18013MobileDocumentRequestContext) -> ParsedRequestSummaryData {
    let documentRequests: [[String: Any]] = requestContext.request.presentmentRequests.flatMap { presentmentRequest in
        presentmentRequest.documentRequestSets.flatMap { documentRequestSet in
            documentRequestSet.requests.map { documentRequest in
                let namespaces = Dictionary(
                    uniqueKeysWithValues: documentRequest.namespaces.map { namespace, elements in
                        return (namespace, elements.mapValues { value in
                            value.isRetaining
                        })
                    }
                )
                return [
                    "docType": documentRequest.documentType,
                    "namespaces": namespaces
                ]
            }
        }
    }
    let summary: [String: Any] = [
        "documentRequests": documentRequests
    ]

    let summaryJson = (try? JSONSerialization.data(withJSONObject: summary))
        .flatMap { String(data: $0, encoding: .utf8) }

    return ParsedRequestSummaryData(
        summaryJson: summaryJson
    )
}

@main
struct DocumentProviderExtension: IdentityDocumentProvider {
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
                versionCode: Bundle.main.infoDictionary?["CFBundleVersion"] as? Int32 ?? 1,
                versionName: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0",
                osVersion: "iOS " + UIDevice.current.systemVersion
            ),
            antilog: OSLogNapierAntilog()
        )
    }

    struct RootViewController: UIViewControllerRepresentable {
        let requestContext: ISO18013MobileDocumentRequestContext

        #if DEBUG
        let buildType = BuildType.debug
        #else
        let buildType = BuildType.release_
        #endif
        
        class Coordinator {
            var requestStarted = false
            // Tracks which requestContext was last wired up so updateUIViewController can detect a new request.
            var lastRequestContext: ISO18013MobileDocumentRequestContext? = nil
        }

        func makeCoordinator() -> Coordinator {
            return Coordinator()
        }

        func makeUIViewController(context: Context) -> StatefulViewController {
            let statefulViewController = StatefulViewController()
            setupForCurrentRequest(statefulViewController: statefulViewController, context: context)
            return statefulViewController
        }

        // iOS may reuse the extension process for a second DC API request without recreating the view
        // controller. In that case SwiftUI calls updateUIViewController instead of makeUIViewController,
        // so we must detect the new requestContext and re-run the full setup.
        func updateUIViewController(_ uiViewController: StatefulViewController, context: Context) {
            if requestContext !== context.coordinator.lastRequestContext {
                setupForCurrentRequest(statefulViewController: uiViewController, context: context)
            }
        }

        private func setupForCurrentRequest(statefulViewController: StatefulViewController, context: Context) {
            // Reset per-request state before wiring up the new request.
            context.coordinator.requestStarted = false
            context.coordinator.lastRequestContext = requestContext

            let originString: String? = requestContext.requestingWebsiteOrigin?.absoluteString
            let parsedRequestSummary = buildParsedRequestSummaryData(from: requestContext)

            let onCancel: () -> Void = {
                Napier.shared.log(priority: LogLevel.debug, tag: "DocumentProviderExtension", throwable: nil, message: "onCancel called")
                IosSessionBridge.shared.clearDcapiPreRequest()
                IosSessionBridge.shared.clearDcapiInvocation()
                requestContext.cancel()
            }

            let onContinue: () -> Void = {
                if context.coordinator.requestStarted {
                    Napier.shared.log(priority: LogLevel.debug, tag: "DocumentProviderExtension", throwable: nil, message: "onContinue already called, ignoring")
                    return
                }
                context.coordinator.requestStarted = true

                IosSessionBridge.shared.clearDcapiPreRequest()
                Napier.shared.log(priority: LogLevel.debug, tag: "DocumentProviderExtension", throwable: nil, message: "onContinue called")
                Task {
                    do {
                        try await requestContext.sendResponse { rawRequest in
                            Napier.shared.log(priority: LogLevel.debug, tag: "DocumentProviderExtension", throwable: nil, message: "sendResponse handler started")
                            Napier.shared.log(priority: LogLevel.debug, tag: "DocumentProviderExtension", throwable: nil, message: "rawRequest: \(String(decoding: rawRequest.requestData, as: UTF8.self))")
                            let finalResponseData = await withCheckedContinuation { continuation in
                                Napier.shared.log(priority: LogLevel.debug, tag: "DocumentProviderExtension", throwable: nil, message: "withCheckedContinuation started")
                                let sendCredentialResponse: (Data?) -> Void = { data in
                                    Napier.shared.log(priority: LogLevel.debug, tag: "DocumentProviderExtension", throwable: nil, message: "sendCredentialResponse called \(String(decoding: data ?? Data(), as: UTF8.self))")
                                    continuation.resume(returning: data ?? Data())
                                }

                                let invocationData = IosDCAPIInvocationData(
                                    rawRequest: String(decoding: rawRequest.requestData, as: UTF8.self),
                                    parsedRequestSummary: parsedRequestSummary.summaryJson,
                                    origin: originString,
                                    sendCredentialResponse: sendCredentialResponse,
                                    onCancel: onCancel
                                )
                                IosSessionBridge.shared.registerDcapiInvocation(data: invocationData)
                            }

                            Napier.shared.log(priority: LogLevel.debug, tag: "DocumentProviderExtension", throwable: nil, message: "sendResponse handler finished")
                            return ISO18013MobileDocumentResponse(responseData: finalResponseData)
                        }
                    } catch {
                        Napier.shared.log(priority: LogLevel.error, tag: "DocumentProviderExtension", throwable: nil, message: "sendResponse failed: \(error)")
                        IosSessionBridge.shared.clearDcapiInvocation()
                        requestContext.cancel()
                    }
                }
            }

            let preRequestData = IosDcApiPreRequestData(
                parsedRequestSummary: parsedRequestSummary.summaryJson,
                origin: originString,
                onContinue: onContinue,
                onCancel: onCancel
            )
            IosSessionBridge.shared.registerDcapiPreRequest(data: preRequestData)

            let mainViewController = Main_iosKt.TransientFlowMainViewController(
                buildContext: BuildContext(
                    buildType: buildType,
                    packageName: Bundle.main.bundleIdentifier ?? "at.asitplus.wallet.compose",
                    versionCode: Bundle.main.infoDictionary?["CFBundleVersion"] as? Int32 ?? 1,
                    versionName: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0",
                    osVersion: "iOS " + UIDevice.current.systemVersion
                )
            )
            statefulViewController.display(viewController: mainViewController)
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
