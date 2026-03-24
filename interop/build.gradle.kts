plugins {
    kotlin("multiplatform")
}

kotlin {
    val isMacHost = System.getProperty("os.name").lowercase().contains("mac")

    val iosTargets = listOf(iosArm64(), iosSimulatorArm64())

    iosTargets.forEach { target ->
        val platform = when (target.name) {
            "iosArm64" -> "iphoneos"
            "iosSimulatorArm64" -> "iphonesimulator"
            else -> error("Unsupported target ${target.name}")
        }

        target.compilations.getByName("main") {
            cinterops {
                val DigitalCredentials by creating {
                    definitionFile.set(file("${rootDir}/cinterop/DigitalCredentials-${platform}.def"))
                    includeDirs.headerFilterOnly("${rootDir}/cinterop/build/Release-${platform}/include")

                    // Only make cinterop processing depend on Xcode builds when on macOS
                    if (isMacHost) {
                        tasks[interopProcessingTaskName].apply {
                            dependsOn(":cinterop:buildIphoneos")
                            dependsOn(":cinterop:buildIphonesimulator")
                        }
                    }
                }
            }
        }

        if (isMacHost) {
            target.binaries.all {
                linkerOpts(
                    "-L/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/${platform}/",
                    "-L${rootDir}/cinterop/build/Release-${platform}",
                    "-lDigitalCredentials"
                )
            }
        }
    }
}
