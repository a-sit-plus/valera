plugins {
    base
}
// Adapted from https://github.com/openwallet-foundation/multipaz
val isMacHost = System.getProperty("os.name").lowercase().contains("mac")

if (isMacHost) {
    listOf("iphoneos", "iphonesimulator").forEach { sdk ->
        val taskName = "build${sdk.replaceFirstChar { it.titlecase() }}"
        val targetTriple = when (sdk) {
            "iphoneos" -> "arm64-apple-ios26.0"
            "iphonesimulator" -> "arm64-apple-ios26.0-simulator"
            else -> error("Unsupported sdk $sdk")
        }
        val releaseDir = "$projectDir/build/Release-$sdk"
        val headerDir = "$releaseDir/include/DigitalCredentials"
        val headerPath = "$headerDir/DigitalCredentials-Swift.h"
        val libraryPath = "$releaseDir/libDigitalCredentials.a"
        val sourceFile = "$projectDir/DigitalCredentials/DigitalCredentials.swift"

        tasks.register<Exec>(taskName) {
            group = "build"
            workingDir = projectDir

            onlyIf { isMacHost }

            commandLine(
                "/bin/bash",
                "-lc",
                """
                set -euo pipefail
                rm -rf "$releaseDir"
                mkdir -p "$headerDir"
                xcrun --sdk $sdk swiftc \
                  -parse-as-library \
                  -module-name DigitalCredentials \
                  -emit-library \
                  -static \
                  -emit-objc-header \
                  -emit-objc-header-path "$headerPath" \
                  -target $targetTriple \
                  -sdk "$(xcrun --sdk $sdk --show-sdk-path)" \
                  "$sourceFile" \
                  -o "$libraryPath"
                """.trimIndent()
            )

            inputs.files(
                fileTree("$projectDir/DigitalCredentials")
            )
            outputs.files(
                fileTree(releaseDir)
            )
        }
    }
}
