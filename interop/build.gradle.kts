plugins {
    kotlin("multiplatform")
}

kotlin {
    val isMacHost = System.getProperty("os.name").lowercase().contains("mac")
    val developerDir = if (isMacHost) {
        project.providers.environmentVariable("DEVELOPER_DIR")
            .map { it.trim() }
            .orElse(
                project.providers.exec {
                    commandLine("xcode-select", "-p")
                }.standardOutput.asText.map { it.trim() }
            )
            .orNull
            ?.ifBlank { null }
    } else {
        null
    }

    val iosTargets = listOf(iosArm64(), iosSimulatorArm64())

    iosTargets.forEach { target ->
        val platform = when (target.name) {
            "iosArm64" -> "iphoneos"
            "iosSimulatorArm64" -> "iphonesimulator"
            else -> error("Unsupported target ${target.name}")
        }
        val platformVersion = when (platform) {
            "iphoneos" -> "ios 26.0 26.0"
            "iphonesimulator" -> "ios-simulator 26.0 26.0"
            else -> error("Unsupported platform $platform")
        }
        val swiftLibPath = developerDir
            ?.let { "$it/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/$platform/" }

        target.compilations.getByName("main") {
            cinterops {
                val DigitalCredentials by creating {
                    definitionFile.set(file("${rootDir}/cinterop/DigitalCredentials-${platform}.def"))
                    includeDirs.headerFilterOnly("${rootDir}/cinterop/build/Release-${platform}/include")
                    if (swiftLibPath != null) {
                        linkerOpts(
                            "-platform_version",
                            *platformVersion.split(" ").toTypedArray(),
                            "-L$swiftLibPath"
                        )
                    }

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
                val opts = buildList {
                    if (swiftLibPath != null) {
                        add("-platform_version")
                        addAll(platformVersion.split(" "))
                        add("-L$swiftLibPath")
                        add("-rpath")
                        add(swiftLibPath)
                    }
                    add("-L${rootDir}/cinterop/build/Release-${platform}")
                    add("-lDigitalCredentials")
                }
                linkerOpts(*opts.toTypedArray())
            }
        }
    }
}
