import at.asitplus.gradle.exportXCFramework
import at.asitplus.gradle.kmmresult
import at.asitplus.gradle.ktor
import at.asitplus.gradle.napier
import at.asitplus.gradle.serialization
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.plugin.extraProperties

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    id("at.asitplus.gradle.conventions")
    id("kotlin-parcelize")
    kotlin("plugin.serialization")
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        instrumentedTestVariant {
            sourceSetTree.set(KotlinSourceSetTree.test)
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.components.resources)
                api(vckOidCatalog.vck.openid.ktor)
                api(libs.credential.mdl)
                api(libs.credential.ida)
                api(libs.credential.eupid)
                api(libs.credential.powerofrepresentation)
                api(libs.credential.certificateofresidence)
                api(libs.credential.companyregistration)
                api(libs.credential.eprescription)
                implementation(serialization("json"))
                api(napier())
                implementation(libs.datastore.preferences.core)
                implementation(libs.datastore.core.okio)
                implementation(libs.navigation.compose)
                api(libs.atomicfu)
                implementation(ktor("client-core"))
                implementation(ktor("client-cio"))
                implementation(ktor("client-logging"))
                implementation(ktor("client-content-negotiation"))
                implementation(ktor("serialization-kotlinx-json"))
                implementation(libs.identity)
                implementation(libs.semver)
                implementation(libs.g0dkar.qrcode.kotlin)
                implementation(libs.gson)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-common"))
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.uiTest)
            }
        }

        androidMain {
            dependencies {
                api(libs.androidx.activity.compose)
                api(libs.androidx.appcompat)
                api(libs.androidx.core.ktx)
                implementation(libs.slf4j.android)
                implementation(ktor("client-android"))
                implementation(libs.camera.camera2)
                implementation(libs.androidx.biometric)
                implementation(libs.androidx.camera.lifecycle)
                implementation(libs.androidx.camera.view)
                implementation(libs.accompanist.permissions)
                implementation(libs.barcode.scanning)
                implementation(libs.play.services.identity.credentials)
                implementation(libs.identity.android)
                implementation(libs.identity.mdoc)
                implementation(libs.okhttp)
                implementation(signumCosefCatalog.indispensable.cosef)
            }
        }

        val androidInstrumentedTest by getting {
            dependencies {
                implementation(libs.ui.test.junit4)
                implementation(libs.ui.test.manifest)
            }
        }
        iosMain { dependencies { implementation(ktor("client-darwin")) } }
    }
}

android {
    compileSdk = (extraProperties["android.compileSdk"] as String).toInt()
    namespace = "at.asitplus.wallet.app.common"

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        minSdk = (extraProperties["android.minSdk"] as String).toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
        resources.excludes += ("META-INF/versions/9/OSGI-INF/MANIFEST.MF")
        resources.excludes.add("**/attach_hotspot_windows.dll")
        resources.excludes.add("META-INF/licenses/**")
        resources.excludes.add("META-INF/AL2.0")
        resources.excludes.add("META-INF/LGPL2.1")
    }
    testOptions {
        managedDevices {
            localDevices {
                create("pixel2api33") {
                    device = "Pixel 2"
                    apiLevel = 33
                    systemImageSource = "aosp-atd"
                }
            }
        }
    }
}

compose.resources {
    packageOfResClass = "at.asitplus.valera.resources"
}

exportXCFramework(
    name = "shared", transitiveExports = false, static = false,
    additionalExports = arrayOf(
        vckCatalog.vck,
        vckOidCatalog.vck.openid,
        vckOidCatalog.vck.openid.ktor,
        libs.credential.ida,
        libs.credential.mdl,
        libs.credential.eupid,
        libs.credential.powerofrepresentation,
        libs.credential.certificateofresidence,
        libs.credential.companyregistration,
        libs.credential.eprescription,
        kmmresult(),
        napier()
    )
) {
    binaryOption("bundleId", "at.asitplus.wallet.shared")
    linkerOpts("-ld_classic")
    freeCompilerArgs += listOf("-Xoverride-konan-properties=minVersion.ios=15.0;minVersionSinceXcode15.ios=15.0")
}

repositories {
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}
