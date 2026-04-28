plugins {
    base
}
// Adapted from https://github.com/openwallet-foundation/multipaz
val isMacHost = System.getProperty("os.name").lowercase().contains("mac")

if (isMacHost) {
    listOf("iphoneos", "iphonesimulator").forEach { sdk ->
        val taskName = "build${sdk.replaceFirstChar { it.titlecase() }}"

        tasks.register<Exec>(taskName) {
            group = "build"
            workingDir = projectDir

            onlyIf { isMacHost }

            commandLine(
                "xcodebuild",
                "-project", "DigitalCredentials.xcodeproj",
                "-scheme", "DigitalCredentials",
                "-sdk", sdk,
                "-configuration", "Release",
                "SYMROOT=${projectDir}/build"
            )

            inputs.files(
                fileTree("$projectDir/DigitalCredentials.xcodeproj") { exclude("**/xcuserdata") },
                fileTree("$projectDir/DigitalCredentials")
            )
            outputs.files(
                fileTree("$projectDir/build/Release-${sdk}")
            )
        }
    }
}