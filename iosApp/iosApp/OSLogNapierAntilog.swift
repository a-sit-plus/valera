
// OSLogNapierAntilog.swift

import Foundation
import OSLog
import shared   // ← replace with your KMP framework module name

/// Forwards Napier logs from Kotlin to Apple's unified logging (OSLog).
final class OSLogNapierAntilog: Antilog {

    private let logger: Logger

    init(
        subsystem: String = Bundle.main.bundleIdentifier ?? "Napier",
        category: String = "Napier"
    ) {
        self.logger = Logger(subsystem: subsystem, category: category)
        super.init()
    }

    override func performLog(
        priority: LogLevel,
        tag: String?,
        throwable: KotlinThrowable?,
        message: String?
    ) {
        let oslogType: OSLogType

        // Map Napier levels → OSLogType.
        // Adjust case names if your generated Swift enums differ.
        switch priority {
        case .verbose:
            oslogType = .debug
        case .debug:
            oslogType = .debug
        case .info:
            oslogType = .info
        case .warning:
            oslogType = .default
        case .error:
            oslogType = .error
        case .assert:
            oslogType = .fault
        default:
            oslogType = .default
        }

        var text = "HIER!!!!!! "

        if let tag, !tag.isEmpty {
            text += "[\(tag)] "
        }

        if let message, !message.isEmpty {
            text += message
        }

        if let throwable {
            // KotlinThrowable has description via NSObject / CustomStringConvertible bridge.
            text += " | throwable=\(throwable)"
        }

        //let logger1 = Logger(subsystem: "Valera", category: "debug")
        //let value = "Hello World"
        //logger1.info("My message: \(value, privacy: .public)")

        logger.log("\(text, privacy: .public)")
        //NSLog("%{public}@", text)
    }
}

