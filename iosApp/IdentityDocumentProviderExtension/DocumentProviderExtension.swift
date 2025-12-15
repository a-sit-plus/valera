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

        func makeUIViewController(context: Context) -> UIViewController {
            Napier.shared.base(antilog:OSLogNapierAntilog())

            // Extract origin URL as String for Kotlin
            let originString: String? = requestContext.requestingWebsiteOrigin?.absoluteString

            requestContext.request.requestAuthentications.first.debugDescription

            Napier.shared.log(priority: LogLevel.debug, tag: "DocumentProviderExtension", throwable: nil, message: "BLABLAB requestContext.requestingWebsiteOrigin: \(originString ?? "nil")")
            Napier.shared.log(priority: LogLevel.debug, tag: "DocumentProviderExtension", throwable: nil, message: "BLABLAB requestContext.request.presentmentRequests: \(requestContext.request.presentmentRequests.first.debugDescription ?? "nil")")
            Napier.shared.log(priority: LogLevel.debug, tag: "DocumentProviderExtension", throwable: nil, message: "BLABLAB requestContext.request.requestAuthentications: \(requestContext.request.requestAuthentications.first.debugDescription ?? "nil")")

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
                        Napier.shared.log(priority: LogLevel.debug, tag: "DocumentProviderExtension", throwable: nil, message: "BLABLAB Calling sendResponse")
                        try await requestContext.sendResponse { rawRequest in
                            Napier.shared.log(priority: LogLevel.debug, tag: "DocumentProviderExtension", throwable: nil, message: "BLABLAB sendResponse handler received rawRequest")
                            Napier.shared.log(priority: LogLevel.debug, tag: "DocumentProviderExtension", throwable: nil, message: "BLABLAB rawRequest.requestData: \(String(decoding: rawRequest.requestData, as: UTF8.self))")
                            // If Kotlin needs to see the request bytes first, we can extend the bridge to pass
                            // rawRequest.requestData to Kotlin and await a response; for now, we already have it.
                            let response = ISO18013MobileDocumentResponse(responseData: payload)
                            return response
                        }
                    } catch {
                        Napier.shared.log(priority: LogLevel.error, tag: "DocumentProviderExtension", throwable: nil, message: "sendResponse failed: \(error)")
                        // If sending a response fails, cancel the session as a fallback
                        requestContext.cancel()
                    }
                }
            }

            let onCancel: () -> Void = {
                requestContext.cancel()
            }

            return Main_iosKt.MdocRequestViewController(
                requestingWebsiteOrigin: originString,
                requestedElements: requestedElements,
                onSendResponse: onSendResponse,
                onCancel: onCancel
            )
        }

        func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        }
    }


    var body: some IdentityDocumentRequestScene {
        ISO18013MobileDocumentRequestScene { context in
            ComposeViewController(requestContext: context)
        }
    }

    func performRegistrationUpdates() async {

    }

}
