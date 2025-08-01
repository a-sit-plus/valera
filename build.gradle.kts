import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family
import java.io.ByteArrayOutputStream

plugins {

    val kotlinVer =
        System.getenv("KOTLIN_VERSION_ENV")?.ifBlank { null } ?: libs.versions.kotlin.get()
    val kotestVer =
        System.getenv("KOTEST_VERSION_ENV")?.ifBlank { null } ?: libs.versions.kotest.get()
    val kspVer = System.getenv("KSP_VERSION_ENV")?.ifBlank { null }
        ?: "$kotlinVer-${libs.versions.ksp.get()}"

    id("at.asitplus.gradle.conventions") version "20250729"
    id("io.kotest") version kotestVer
    kotlin("multiplatform") version kotlinVer apply false
    kotlin("plugin.serialization") version kotlinVer apply false
    id("com.google.devtools.ksp") version kspVer

    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    id("com.android.application") apply (false)
    id("com.android.library") apply (false)

    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.compose.compiler) version (vckOidCatalog.versions.kotlin) apply false
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
                            if (konanTarget.name.contains(
                                    "SIMULATOR",
                                    true
                                )
                            ) "iphonesimulator" else "iphoneos"

                        Family.OSX -> "macosx"
                        Family.TVOS ->
                            if (konanTarget.name.contains(
                                    "SIMULATOR",
                                    true
                                )
                            ) "appletvsimulator" else "appletvos"

                        Family.WATCHOS ->
                            if (konanTarget.name.contains(
                                    "SIMULATOR",
                                    true
                                )
                            ) "watchsimulator" else "watchos"

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