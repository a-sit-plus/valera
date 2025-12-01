//
// Created by Gerald P. on 25.11.25.
// Copyright (c) 2025 orgName. All rights reserved.
//

/*/import Foundation
import os.log
import shared

@objc public class OsLogBridge: NSObject {
    @objc public static func log(
        level: String,
        tag: String?,
        message: String
    ) {
        
        
        let subsystem = Bundle.main.bundleIdentifier ?? "KotlinShared"
        let category = tag ?? "Default"

        let logger = Logger(subsystem: subsystem, category: category)
        logger.debug("HIER!!!!!!!!!!!!!!!!!")
        logger.log("HIER!!!!!!!!!!!!!!!!!2")

        switch level {
        case "DEBUG":
            logger.debug("\(message, privacy: .auto)")
        case "INFO":
            logger.info("\(message, privacy: .auto)")
        case "WARN":
            logger.warning("\(message, privacy: .auto)")
        case "ERROR":
            logger.error("\(message, privacy: .auto)")
        default:
            logger.log("\(message, privacy: .auto)")
        }
    }
}*/
