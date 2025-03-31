import at.asitplus.gradle.exportXCFramework
import at.asitplus.gradle.kmmresult
import at.asitplus.gradle.ktor
import at.asitplus.gradle.napier
import at.asitplus.gradle.serialization
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.plugin.extraProperties

val vckVersion = vckCatalog.vck.get().version

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
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation("at.asitplus.wallet:vck-rqes:$vckVersion")
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.components.resources)
            api(vckOidCatalog.vck.openid.ktor)
            api(libs.credential.mdl)
            api(libs.credential.ida)
            api(libs.credential.eupid)
            api(libs.credential.powerofrepresentation)
            api(libs.credential.certificateofresidence)
            api(libs.credential.companyregistration)
            api(libs.credential.healthid)
            api(libs.credential.taxid)
            implementation(serialization("json"))
            api(napier())
            implementation("androidx.datastore:datastore-preferences-core:1.1.1")
            implementation("androidx.datastore:datastore-core-okio:1.1.1")
            implementation("org.jetbrains.androidx.navigation:navigation-compose:2.8.0-alpha10")
            api(libs.atomicfu)
            implementation(ktor("client-core"))
            implementation(ktor("client-cio"))
            implementation(ktor("client-logging"))
            implementation(ktor("client-content-negotiation"))
            implementation(ktor("serialization-kotlinx-json"))

            implementation(libs.multipaz)
            implementation(libs.multipaz.doctypes)
            implementation(libs.semver)

        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(kotlin("test-common"))
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }

        androidMain.dependencies {
            implementation("androidx.biometric:biometric:1.2.0-alpha05")
            api("androidx.activity:activity-compose:1.8.1")
            api("androidx.appcompat:appcompat:1.6.1")
            api("androidx.core:core-ktx:1.12.0")
            implementation("uk.uuid.slf4j:slf4j-android:1.7.30-0")
            implementation(ktor("client-android"))
            implementation("androidx.camera:camera-camera2:1.3.0")
            implementation("androidx.camera:camera-lifecycle:1.3.0")
            implementation("androidx.camera:camera-view:1.3.0")
            implementation("com.google.accompanist:accompanist-permissions:0.30.1")
            implementation("com.google.mlkit:barcode-scanning:17.2.0")
            implementation(libs.play.services.identity.credentials)
            implementation(libs.multipaz.android.legacy)
        }

        androidInstrumentedTest.dependencies {
            implementation("androidx.compose.ui:ui-test-junit4")
            implementation("androidx.compose.ui:ui-test-manifest")
        }
        iosMain.dependencies { implementation(ktor("client-darwin")) }
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
        libs.credential.healthid,
        libs.credential.taxid,
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
