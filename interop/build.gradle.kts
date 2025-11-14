plugins {
    kotlin("multiplatform")
}

kotlin {
    val iosTargets = listOf(iosX64(), iosArm64(), iosSimulatorArm64())

    iosTargets.forEach { target ->
        val platform = when (target.name) {
            "iosX64" -> "iphonesimulator"
            "iosArm64" -> "iphoneos"
            "iosSimulatorArm64" -> "iphonesimulator"
            else -> error("Unsupported target ${target.name}")
        }

        target.compilations.getByName("main") {
            cinterops {
                val DigitalCredentials by creating {
                    definitionFile.set(file("${rootDir}/cinterop/DigitalCredentials-${platform}.def"))
                    includeDirs.headerFilterOnly("${rootDir}/cinterop/build/Release-${platform}/include")

                    tasks[interopProcessingTaskName].apply {
                        dependsOn(":cinterop:buildIphoneos")
                        dependsOn(":cinterop:buildIphonesimulator")
                    }
                }
            }
        }

        target.binaries.all {
            linkerOpts(
                "-L/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/${platform}/",
                "-L${rootDir}/cinterop/build/Release-${platform}",
                "-lDigitalCredentials"
            )
        }
    }
}