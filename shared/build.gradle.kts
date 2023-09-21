import at.asitplus.gradle.*

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("at.asitplus.gradle.conventions")
    id("kotlin-parcelize")
}

kotlin {
    androidTarget()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
            export("at.asitplus.wallet:vclib:3.0.1")
            export("at.asitplus:kmmresult:1.5.3")
            export(napier())
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.components.resources)
                api("at.asitplus.wallet:vclib:3.0.1")
                api(napier())
            }
        }
        val androidMain by getting {
            dependencies {
                api("androidx.activity:activity-compose:1.7.2")
                api("androidx.appcompat:appcompat:1.6.1")
                api("androidx.core:core-ktx:1.10.1")

                implementation("androidx.camera:camera-camera2:1.2.2")
                implementation("androidx.camera:camera-lifecycle:1.2.2")
                implementation("androidx.camera:camera-view:1.2.2")
                implementation("com.google.accompanist:accompanist-permissions:0.29.2-rc")
                implementation("com.google.android.gms:play-services-maps:18.1.0")
                implementation("com.google.android.gms:play-services-location:21.0.1")
                implementation("com.google.maps.android:maps-compose:2.11.2")
                implementation("com.google.mlkit:barcode-scanning:17.2.0")
            }
        }
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
    }
}

android {
    compileSdk = (findProperty("android.compileSdk") as String).toInt()
    namespace = "at.asitplus.wallet.app.common"

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        minSdk = (findProperty("android.minSdk") as String).toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
}
