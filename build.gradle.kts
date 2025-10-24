import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family
import java.io.ByteArrayOutputStream

plugins {
    val kotlinVer =
        System.getenv("KOTLIN_VERSION_ENV")?.ifBlank { null } ?: libs.versions.kotlin.get()

    id("at.asitplus.gradle.conventions") version "20251017"
    kotlin("multiplatform") version kotlinVer apply false
    kotlin("plugin.serialization") version kotlinVer apply false

    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    id("com.android.application") version libs.versions.agp.get() apply (false)
    id("com.android.library") version libs.versions.agp.get() apply (false)

    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.compose.compiler) version kotlinVer apply false
}

subprojects {
    afterEvaluate {
        /*help the linker (yes, this is absolutely bonkers!)*/
        if (org.gradle.internal.os.OperatingSystem.current() == org.gradle.internal.os.OperatingSystem.MAC_OS) {
            val devDir = System.getenv("DEVELOPER_DIR")?.ifEmpty { null }.let {
                if (it == null) {
                    val output = ByteArrayOutputStream()
                    project.exec {
                        commandLine("xcode-select", "-p")
                        standardOutput = output
                    }
                    output.toString().trim()
                } else it
            }

            logger.lifecycle("  DEV DIR points to $devDir")
            val swiftLib = "$devDir/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/"

            extensions.getByType<KotlinMultiplatformExtension>().targets.withType<KotlinNativeTarget>()
                .configureEach {
                    val sub = when (konanTarget.family) {
                        Family.IOS ->
                            if (konanTarget.name.contains("SIMULATOR", true)) "iphonesimulator" else "iphoneos"
                        Family.TVOS ->
                            if (konanTarget.name.contains("SIMULATOR", true)) "appletvsimulator" else "appletvos"
                        Family.WATCHOS ->
                            if (konanTarget.name.contains("SIMULATOR", true)) "watchsimulator" else "watchos"
                        Family.OSX -> "macosx"
                        else -> throw StopExecutionException("Konan target ${konanTarget.name} is not recognized")
                    }

                    logger.lifecycle("  KONAN target is ${konanTarget.name} which resolves to $sub")
                    binaries.all {
                        linkerOpts(
                            "-L${swiftLib}$sub",
                            "-L/usr/lib/swift"
                        )
                    }
                }
        }
    }
}