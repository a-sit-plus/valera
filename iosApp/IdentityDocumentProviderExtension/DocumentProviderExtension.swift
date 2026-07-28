import ExtensionKit
import Foundation
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

private struct ParsedRequestSummary: Encodable {
    let documentRequests: [ParsedDocumentRequest]
}

private struct ParsedDocumentRequest: Encodable {
    let docType: String
    let namespaces: [String: [String: Bool]]
}

private struct DcApiValidationError: LocalizedError {
    let message: String

    var errorDescription: String? {
        message
    }
}

// Kotlin completes a presentment asynchronously after the sendResponse closure has returned
// control to the shared UI. This gate makes all response, validation-error, and cancellation
// callbacks idempotent so the checked continuation is resumed at most once.
private final class DcApiResponseCompletion: @unchecked Sendable {
    private let lock = NSLock()
    private var continuation: CheckedContinuation<Data, Error>?

    init(_ continuation: CheckedContinuation<Data, Error>) {
        self.continuation = continuation
    }

    func returnResponse(_ data: Data) {
        takeContinuation()?.resume(returning: data)
    }

    func failValidation(_ message: String) {
        takeContinuation()?.resume(throwing: DcApiValidationError(message: message))
    }

    func cancel() {
        takeContinuation()?.resume(throwing: CancellationError())
    }

    private func takeContinuation() -> CheckedContinuation<Data, Error>? {
        lock.lock()
        defer { lock.unlock() }
        let value = continuation
        continuation = nil
        return value
    }
}

// The extension process can be reused across requests, so we need a stable fingerprint for
// "same request vs. new request" detection that does not depend on object identity.
private func buildRequestSignature(from requestContext: ISO18013MobileDocumentRequestContext) -> String {
    let parsedRequestSummary = buildParsedRequestSummaryData(from: requestContext)
    let originString = requestContext.requestingWebsiteOrigin?.absoluteString ?? ""
    return originString + "|" + (parsedRequestSummary.summaryJson ?? "")
}

private func buildParsedRequestSummaryData(from requestContext: ISO18013MobileDocumentRequestContext) -> ParsedRequestSummaryData {
    let documentRequests: [ParsedDocumentRequest] = requestContext.request.presentmentRequests.flatMap { presentmentRequest in
        presentmentRequest.documentRequestSets.flatMap { documentRequestSet in
            documentRequestSet.requests.map { documentRequest in
                // Sort namespaces and elements before encoding so the resulting JSON is stable
                // across repeated setup calls for the same request.
                let namespaces = Dictionary(
                    uniqueKeysWithValues: documentRequest.namespaces
                        .sorted { lhs, rhs in lhs.key < rhs.key }
                        .map { namespace, elements in
                            let sortedElements = Dictionary(
                                uniqueKeysWithValues: elements
                                    .sorted { lhs, rhs in lhs.key < rhs.key }
                                    .map { elementName, value in
                                        (elementName, value.isRetaining)
                                    }
                            )
                            return (namespace, sortedElements)
                        }
                )
                return ParsedDocumentRequest(
                    docType: documentRequest.documentType,
                    namespaces: namespaces
                )
            }
        }
    }
    let summary = ParsedRequestSummary(documentRequests: documentRequests)
    let encoder = JSONEncoder()
    if #available(iOS 11.0, *) {
        encoder.outputFormatting = [.sortedKeys]
    }
    let summaryJson = (try? encoder.encode(summary))
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
                // CFBundleVersion is always a String in modern bundles; cast it to String
                // first and then convert — a direct `as? Int32` cast always fails and falls back to 1.
                versionCode: (Bundle.main.infoDictionary?["CFBundleVersion"] as? String).flatMap { Int32($0) } ?? 1,
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
            // Tracks the last request fingerprint so updateUIViewController can detect a new request.
            var lastRequestSignature: String? = nil
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
            let requestSignature = buildRequestSignature(from: requestContext)
            if requestSignature != context.coordinator.lastRequestSignature {
                setupForCurrentRequest(statefulViewController: uiViewController, context: context)
            }
        }

        private func markRequestFinished(context: Context) {
            context.coordinator.requestStarted = false
            context.coordinator.lastRequestSignature = nil
        }

        private func setupForCurrentRequest(statefulViewController: StatefulViewController, context: Context) {
            // The extension process is reused across DC API requests, so the cached transient
            // session keeps a stale DataStore snapshot (DataStore is single-process and never
            // observes the main app's cross-process writes). Close it so the session below is
            // rebuilt with a fresh DataStore that reads newly issued credentials from disk.
            IosSessionBridge.shared.closeTransientFlowSession()

            // Reset per-request state before wiring up the new request.
            context.coordinator.requestStarted = false
            context.coordinator.lastRequestSignature = buildRequestSignature(from: requestContext)

            let originString: String? = requestContext.requestingWebsiteOrigin?.absoluteString
            let parsedRequestSummary = buildParsedRequestSummaryData(from: requestContext)

            let cancelRequest: () -> Void = {
                Napier.shared.log(priority: LogLevel.debug, tag: "DocumentProviderExtension", throwable: nil, message: "onCancel called")
                // Clear Kotlin-side transient state before cancelling the system request so the next
                // external flow starts from a clean bridge state.
                markRequestFinished(context: context)
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

                // Once the user accepts the summary screen, replace the pre-request bridge object
                // with the actual invocation state that will serve the credential response.
                IosSessionBridge.shared.clearDcapiPreRequest()
                Napier.shared.log(priority: LogLevel.debug, tag: "DocumentProviderExtension", throwable: nil, message: "onContinue called")
                Task {
                    defer {
                        markRequestFinished(context: context)
                        IosSessionBridge.shared.clearDcapiInvocation()
                    }
                    do {
                        try await requestContext.sendResponse { rawRequest in
                            Napier.shared.log(priority: LogLevel.debug, tag: "DocumentProviderExtension", throwable: nil, message: "sendResponse handler started")
                            Napier.shared.log(priority: LogLevel.debug, tag: "DocumentProviderExtension", throwable: nil, message: "rawRequest: \(String(decoding: rawRequest.requestData, as: UTF8.self))")
                            let finalResponseData = try await withCheckedThrowingContinuation { continuation in
                                Napier.shared.log(priority: LogLevel.debug, tag: "DocumentProviderExtension", throwable: nil, message: "withCheckedThrowingContinuation started")
                                let completion = DcApiResponseCompletion(continuation)
                                let sendCredentialResponse: (Data) -> Void = { data in
                                    Napier.shared.log(priority: LogLevel.debug, tag: "DocumentProviderExtension", throwable: nil, message: "sendCredentialResponse called with \(data.count) bytes")
                                    completion.returnResponse(data)
                                }
                                let sendCredentialError: (String) -> Void = { message in
                                    Napier.shared.log(priority: LogLevel.error, tag: "DocumentProviderExtension", throwable: nil, message: "sendCredentialError called: \(message)")
                                    completion.failValidation(message)
                                }
                                let cancelInvocation: () -> Void = {
                                    completion.cancel()
                                    cancelRequest()
                                }

                                // Hand the raw verifier request into shared Kotlin code, which drives
                                // the actual consent/authentication flow and eventually calls back
                                // with ISO response bytes, a validation failure, or explicit cancellation.
                                let invocationData = IosDCAPIInvocationData(
                                    rawRequest: String(decoding: rawRequest.requestData, as: UTF8.self),
                                    parsedRequestSummary: parsedRequestSummary.summaryJson,
                                    origin: originString,
                                    sendCredentialResponse: sendCredentialResponse,
                                    sendCredentialError: sendCredentialError,
                                    onCancel: cancelInvocation
                                )
                                IosSessionBridge.shared.registerDcapiInvocation(data: invocationData)
                            }

                            Napier.shared.log(priority: LogLevel.debug, tag: "DocumentProviderExtension", throwable: nil, message: "sendResponse handler finished")
                            return ISO18013MobileDocumentResponse(responseData: finalResponseData)
                        }
                    } catch is CancellationError {
                        Napier.shared.log(priority: LogLevel.debug, tag: "DocumentProviderExtension", throwable: nil, message: "sendResponse cancelled")
                    } catch {
                        Napier.shared.log(priority: LogLevel.error, tag: "DocumentProviderExtension", throwable: nil, message: "sendResponse failed: \(error)")
                    }
                }
            }

            // Expose the lightweight pre-request summary first so the UI can show the relying
            // party and requested attributes before the verifier request is fully resumed.
            let preRequestData = IosDcApiPreRequestData(
                parsedRequestSummary: parsedRequestSummary.summaryJson,
                origin: originString,
                onContinue: onContinue,
                onCancel: cancelRequest
            )
            IosSessionBridge.shared.registerDcapiPreRequest(data: preRequestData)

            let mainViewController = Main_iosKt.TransientFlowMainViewController(
                buildContext: BuildContext(
                    buildType: buildType,
                    packageName: Bundle.main.bundleIdentifier ?? "at.asitplus.wallet.compose",
                    // CFBundleVersion is always a String in modern bundles; cast it to String
                // first and then convert — a direct `as? Int32` cast always fails and falls back to 1.
                versionCode: (Bundle.main.infoDictionary?["CFBundleVersion"] as? String).flatMap { Int32($0) } ?? 1,
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
