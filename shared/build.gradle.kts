import at.asitplus.gradle.envExtra
import at.asitplus.gradle.exportXCFramework
import at.asitplus.gradle.ktor
import at.asitplus.gradle.kmmresult
import at.asitplus.gradle.napier
import at.asitplus.gradle.serialization
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest
import java.io.ByteArrayOutputStream
import java.io.File


 plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    id("at.asitplus.gradle.conventions")
    id("kotlin-parcelize")
    kotlin("plugin.serialization")
    id("de.infix.testBalloon")
}

val disableAppleTargets by envExtra
val iosLinkerOpts = listOf("-lsqlite3")
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
val iosSimulatorSwiftLibPath = developerDir
    ?.let { "$it/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/iphonesimulator/" }
val iosDeviceSwiftLibPath = developerDir
    ?.let { "$it/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/iphoneos/" }
val swiftStdLibToolPath = developerDir
    ?.let {
        sequenceOf(
            "$it/Toolchains/XcodeDefault.xctoolchain/usr/bin/builtin-swiftStdLibTool",
            "$it/usr/bin/builtin-swiftStdLibTool",
        ).map(::File).firstOrNull(File::exists)
    }

fun commandOutput(vararg command: String): String {
    val stdout = ByteArrayOutputStream()
    val process = ProcessBuilder(*command)
        .redirectErrorStream(true)
        .start()

    process.inputStream.use { input ->
        input.copyTo(stdout)
    }

    check(process.waitFor() == 0) {
        "Command `${command.joinToString(" ")}` failed:\n${stdout.toString()}"
    }

    return stdout.toString().trim()
}

fun tryCommand(vararg command: String): Result<String> = runCatching {
    commandOutput(*command)
}

fun referencedSwiftDylibs(binary: File): Set<String> {
    val dylibNameRegex = Regex("""(?:^|/)(libswift_[^\s/]+\.dylib)\s""")
    return commandOutput("otool", "-L", binary.absolutePath)
        .lineSequence()
        .mapNotNull { line -> dylibNameRegex.find(line.trim())?.groupValues?.get(1) }
        .toSet()
}

fun findSwiftDylib(name: String, searchRoots: List<File>): File? {
    var fallback: File? = null
    searchRoots.filter(File::exists).forEach { root ->
        root.walkTopDown().forEach { candidate ->
            if (!candidate.isFile || candidate.name != name) {
                return@forEach
            }
            if (candidate.path.contains("iphonesimulator", ignoreCase = true)) {
                return candidate
            }
            if (fallback == null) {
                fallback = candidate
            }
        }
    }
    return fallback
}

fun simulatorRuntimeProvidesSwiftDylib(name: String, runtimeRoots: List<File>): Boolean =
    runtimeRoots.any { root ->
        sequenceOf(
            root.resolve("usr/lib/swift/$name"),
            root.resolve("usr/lib/$name"),
        ).any(File::exists)
    }

fun collectSimulatorRuntimeRoots(): List<File> =
    listOf(
        File("/Library/Developer/CoreSimulator/Profiles/Runtimes"),
        File("/Library/Developer/CoreSimulator/Volumes"),
    )
        .filter(File::exists)
        .flatMap { base ->
            base.walkTopDown()
                .filter { candidate ->
                    candidate.isDirectory && candidate.path.endsWith("/Contents/Resources/RuntimeRoot")
                }
                .toList()
        }
        .distinctBy { it.absolutePath }

kotlin {
    jvmToolchain(17)
    androidLibrary {
        namespace = "at.asitplus.wallet.app.common"

        packaging {
            resources.excludes += ("META-INF/versions/9/OSGI-INF/MANIFEST.MF")
            resources.excludes.add("**/attach_hotspot_windows.dll")
            resources.excludes.add("META-INF/licenses/**")
            resources.excludes.add("META-INF/AL2.0")
            resources.excludes.add("META-INF/LGPL2.1")
        }

        androidResources {
            enable = true
        }

        withHostTest {
            isIncludeAndroidResources = true
        }

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunnerArguments["timeout_msec"] = "2400000"
            managedDevices {
                localDevices {
                    create("pixel2api35") {
                        device = "Pixel 2"
                        apiLevel = 35
                        systemImageSource = "aosp-atd"
                    }
                }
            }
        }
    }
    jvmToolchain(17)

    if ("true" != disableAppleTargets) {
        iosArm64().binaries.all {
            val opts = buildList {
                addAll(iosLinkerOpts)
                if (iosDeviceSwiftLibPath != null) {
                    add("-rpath")
                    add(iosDeviceSwiftLibPath)
                }
            }
            linkerOpts(*opts.toTypedArray())
        }
        iosSimulatorArm64().binaries.all {
            val opts = buildList {
                addAll(iosLinkerOpts)
                if (iosSimulatorSwiftLibPath != null) {
                    add("-rpath")
                    add(iosSimulatorSwiftLibPath)
                }
            }
            linkerOpts(*opts.toTypedArray())
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.datetime.compat)

            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.components.resources)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(libs.back.handler)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            api(libs.vck.openid.ktor)
            api(libs.atomicfu)
            api(libs.credential.mdl)
            api(libs.credential.ida)
            api(libs.credential.eupid)
            api(libs.credential.av)
            api(libs.credential.eupid.sdjwt)
            api(libs.credential.powerofrepresentation)
            api(libs.credential.certificateofresidence)
            api(libs.credential.companyregistration)
            api(libs.credential.healthid)
            api(libs.credential.taxid)
            api(libs.credential.ehic)
            api(napier())
            api(kmmresult())
            implementation(serialization("json"))
            implementation(ktor("client-core"))
            implementation(ktor("client-cio"))
            implementation(ktor("client-logging"))
            implementation(ktor("client-content-negotiation"))
            implementation(ktor("serialization-kotlinx-json"))
            implementation(libs.datastore.preferences.core)
            implementation(libs.datastore.core.okio)
            implementation(libs.multipaz) // This is the library bringing in Bouncy Castle
            implementation(libs.multipaz.compose)
            implementation(libs.multipaz.doctypes)
            implementation(libs.navigation.compose)
            implementation(libs.semver)
            implementation(libs.qrose)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.viewmodel.navigation)
            implementation(libs.authcheckkit)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(kotlin("test-common"))
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
            implementation(libs.koin.test)
            implementation(libs.testballoon)
        }

        androidMain.dependencies {
            implementation(libs.androidx.biometric)
            api(libs.androidx.activity.compose)
            api(libs.androidx.appcompat)
            api(libs.androidx.core.ktx)
            implementation("uk.uuid.slf4j:slf4j-android:1.7.30-0")
            implementation(ktor("client-android"))
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)
            implementation(libs.accompanist.permissions)
            implementation(libs.barcode.scanning)
            implementation(libs.core.splashscreen)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.registry.provider)
            implementation(libs.androidx.registry.provider.play.services)
            implementation(libs.androidx.browser)
        }

        getByName("androidDeviceTest").dependencies {
            implementation("androidx.compose.ui:ui-test-junit4")
            implementation("androidx.compose.ui:ui-test-manifest")
        }
        iosMain.dependencies {
            implementation(ktor("client-darwin"))
        }
    }
}


compose.resources {
    packageOfResClass = "at.asitplus.valera.resources"
}

exportXCFramework(
    name = "shared", transitiveExports = false, static = true,
    additionalExports = arrayOf(
        libs.vck,
        libs.vck.openid,
        libs.vck.openid.ktor,
        libs.credential.ida,
        libs.credential.mdl,
        libs.credential.av,
        libs.credential.eupid,
        libs.credential.eupid.sdjwt,
        libs.credential.powerofrepresentation,
        libs.credential.certificateofresidence,
        libs.credential.companyregistration,
        libs.credential.healthid,
        libs.credential.ehic,
        libs.credential.taxid,
        kmmresult(),
        napier()
    )
) {
    binaryOption("bundleId", "at.asitplus.wallet.shared")
    freeCompilerArgs += listOf("-Xoverride-konan-properties=minVersion.ios=18.5;minVersionSinceXcode15.ios=18.5")
}

if ("true" != disableAppleTargets) {
    if (isMacHost && developerDir != null) {
        val iosSimulatorLinkTask = tasks.named("linkDebugTestIosSimulatorArm64", KotlinNativeLink::class.java)
        val copySwiftRuntimeForIosSimulatorTest = tasks.register("copySwiftRuntimeForIosSimulatorTest") {
            dependsOn(iosSimulatorLinkTask)

            val testExecutable = iosSimulatorLinkTask.flatMap { it.outputFile }
            inputs.file(testExecutable)
            inputs.property("developerDir", developerDir)

            doLast {
                val executable = testExecutable.get()
                val frameworksDir = executable.parentFile.resolve("Frameworks")
                val runtimeRoots = collectSimulatorRuntimeRoots()
                val searchRoots = listOf(
                    file("$developerDir/Toolchains/XcodeDefault.xctoolchain/usr/lib"),
                    file("$developerDir/Platforms/iPhoneSimulator.platform"),
                ) + runtimeRoots

                delete(frameworksDir)
                frameworksDir.mkdirs()

                val swiftStdLibToolResult = swiftStdLibToolPath?.let { tool ->
                    tryCommand(
                        tool.absolutePath,
                        "--copy",
                        "--verbose",
                        "--scan-executable",
                        executable.absolutePath,
                        "--scan-folder",
                        frameworksDir.absolutePath,
                        "--platform",
                        "iphonesimulator",
                        "--toolchain",
                        "$developerDir/Toolchains/XcodeDefault.xctoolchain",
                        "--destination",
                        frameworksDir.absolutePath,
                        "--back-deploy-swift-span",
                    )
                } ?: Result.failure(IllegalStateException("Could not locate builtin-swiftStdLibTool under $developerDir"))

                val copiedByTool = frameworksDir.resolve("libswift_Concurrency.dylib").exists()
                if (!copiedByTool) {
                    swiftStdLibToolResult.exceptionOrNull()?.let { error ->
                        logger.warn("builtin-swiftStdLibTool failed for $executable; falling back to manual Swift runtime copy.", error)
                    }

                    val copied = mutableSetOf<String>()
                    val pending = ArrayDeque(referencedSwiftDylibs(executable))

                    while (pending.isNotEmpty()) {
                        val dylibName = pending.removeFirst()
                        if (!copied.add(dylibName)) {
                            continue
                        }
                        if (simulatorRuntimeProvidesSwiftDylib(dylibName, runtimeRoots)) {
                            continue
                        }

                        val source = findSwiftDylib(dylibName, searchRoots)
                            ?: error("Could not locate $dylibName in simulator Swift runtime paths for $developerDir.")
                        val destination = frameworksDir.resolve(dylibName)
                        source.copyTo(destination, overwrite = true)

                        referencedSwiftDylibs(destination)
                            .filterNot(copied::contains)
                            .filterNot { frameworksDir.resolve(it).exists() }
                            .forEach(pending::addLast)
                    }
                }

                frameworksDir.listFiles()
                    ?.filter { candidate ->
                        candidate.isFile &&
                            candidate.name.startsWith("libswift_") &&
                            candidate.extension == "dylib" &&
                            simulatorRuntimeProvidesSwiftDylib(candidate.name, runtimeRoots)
                    }
                    ?.forEach { duplicate ->
                        logger.lifecycle(
                            "Removing simulator-provided Swift runtime library ${duplicate.name} from ${frameworksDir.absolutePath}"
                        )
                        duplicate.delete()
                    }
            }
        }

        tasks.named("iosSimulatorArm64Test", KotlinNativeSimulatorTest::class.java).configure {
            dependsOn(copySwiftRuntimeForIosSimulatorTest)
            device.set("iPhone 16")
        }
    } else {
        tasks.named("iosSimulatorArm64Test", KotlinNativeSimulatorTest::class.java).configure {
            device.set("iPhone 16")
        }
    }
}

tasks.register("findDependency") {
    group = "help"
    description = "Lists every configuration that resolves the given module"
    val target = project.providers.gradleProperty("module")

    doLast {
        val wanted = target.getOrElse("").takeIf { it.isNotBlank() }
            ?: error("Pass -Pmodule=<group:name>")

        configurations
            .filter { it.isCanBeResolved }
            .forEach { cfg ->
                val result = cfg.incoming.resolutionResult.allComponents
                    .any { c -> c.moduleVersion?.let { "${it.group}:${it.name}" } == wanted }

                if (result) println("${project.path}:${cfg.name}")
            }
    }
}
