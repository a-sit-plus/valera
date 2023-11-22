//
//  Compose_WalletUITests.swift
//  Compose WalletUITests
//
//  Created by Michael Dietrich on 21.11.23.
//  Copyright © 2023 orgName. All rights reserved.
//

import XCTest
import shared

final class Compose_WalletUITests: XCTestCase {

    override func setUpWithError() throws {
        // Put setup code here. This method is called before the invocation of each test method in the class.

        // In UI tests it is usually best to stop immediately when a failure occurs.
        continueAfterFailure = false

        // In UI tests it’s important to set the initial state - such as interface orientation - required for your tests before they run. The setUp method is a good place to do this.
    }

    override func tearDownWithError() throws {
        // Put teardown code here. This method is called after the invocation of each test method in the class.
    }

    func testStartup() throws {
        // UI tests must launch the application that they test.
        print(AppKt.iosTestValue)
        let app = XCUIApplication()
        app.launch()
        XCUIApplication().windows.children(matching: .other).element.tap()
        // more is not possible, but will probaby be possible in 2024 with a Kotlin Multiplatform Accessibility update
    }

    func testLaunchPerformance() throws {
        if #available(macOS 10.15, iOS 13.0, tvOS 13.0, watchOS 7.0, *) {
            // This measures how long it takes to launch your application.
            measure(metrics: [XCTApplicationLaunchMetric()]) {
                XCUIApplication().launch()
            }
        }
    }
}
