import at.asitplus.gradle.exportXCFramework
import at.asitplus.gradle.ktor
import at.asitplus.gradle.kmmresult
import at.asitplus.gradle.napier
import at.asitplus.gradle.serialization
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest


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

kotlin {
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

    iosX64()
    iosArm64()
    iosSimulatorArm64()


    sourceSets {
        commonMain.dependencies {
            implementation(libs.datetime.compat)

            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.components.resources)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(libs.vck.rqes)
            api(libs.vck.openid.ktor)
            api(libs.atomicfu)
            api(libs.credential.mdl)
            api(libs.credential.ida)
            api(libs.credential.eupid)
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
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(kotlin("test-common"))
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
            implementation(libs.koin.test)
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

            // bcpkix-jdk18on is included in signum which enforces to a specific version
            implementation("org.multipaz:multipaz-android-legacy:0.92.0") {
                exclude(group = "org.bouncycastle", module = "bcpkix-jdk18on")
            }
            implementation(libs.core.splashscreen)

            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.registry.provider)
            implementation(libs.androidx.registry.provider.play.services)
        }

        getByName("androidDeviceTest").dependencies {
            implementation("androidx.compose.ui:ui-test-junit4")
            implementation("androidx.compose.ui:ui-test-manifest")
        }
        iosMain.dependencies { implementation(ktor("client-darwin")) }
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
    linkerOpts("-ld_classic")
    freeCompilerArgs += listOf("-Xoverride-konan-properties=minVersion.ios=15.0;minVersionSinceXcode15.ios=15.0")
}

tasks.register("iosBootSimulator") {
    doLast {
        exec {
            isIgnoreExitValue = true
            runCatching {
                commandLine("xcrun", "simctl", "boot", "iPhone 16")
            }
        }
    }
}

tasks.named("iosSimulatorArm64Test", KotlinNativeSimulatorTest::class.java).configure {
    dependsOn("iosBootSimulator")
    standalone.set(false)
    device.set("iPhone 16")
}

tasks.register("findDependency") {
    group = "help"
    description = "Lists every configuration that resolves the given module"
    val target = project.providers.gradleProperty("module").forUseAtConfigurationTime()

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